package io.github.alien.roseau.git;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import io.github.alien.roseau.extractors.incremental.IncrementalTypesExtractor;
import io.github.alien.roseau.extractors.jdt.IncrementalJdtTypesExtractor;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import io.github.alien.roseau.options.RoseauOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Walks a Git repository's first-parent commit chain, building APIs incrementally and
 * computing breaking changes between consecutive commits. Results are emitted to a
 * {@link CommitSink} for each processed commit.
 *
 * <p>Configured once at construction; all walk state is local to {@link #walk}, so a
 * single instance can be reused across multiple calls.</p>
 */
public final class GitWalker {
	private static final Logger LOGGER = LogManager.getLogger(GitWalker.class);

	/**
	 * Configuration for a repository walk.
	 */
	public record Config(
		String libraryId,
		String url,
		Path gitDir,
		List<Path> sourceRoots,
		RoseauOptions.Exclude exclusions
	) {
	}

	/**
	 * Receives {@link CommitAnalysis} results during a walk.
	 */
	@FunctionalInterface
	public interface CommitSink {
		void accept(CommitAnalysis analysis) throws Exception;
	}

	private final Config config;

	public GitWalker(Config config) {
		this.config = config;
	}

	/**
	 * Walks the first-parent commit chain, calling {@code sink} for every commit that
	 * can be associated with an API. Automatically retries once if the repository has
	 * missing objects (re-clones and restarts the walk).
	 */
	public void walk(CommitSink sink) throws Exception {
		try {
			walkOnce(sink);
		} catch (Exception e) {
			if (!RepositoryWalkerUtils.isMissingObjectFailure(e)) {
				throw e;
			}
			LOGGER.warn("Repository {} has missing objects, re-cloning and retrying", config.gitDir(), e);
			RepositoryWalkerUtils.recloneRepository(config.url(), config.gitDir());
			walkOnce(sink);
		}
	}

	/**
	 * Walks and writes the standard two-CSV output to {@code outputDir}.
	 */
	public void walkToCsv(Path outputDir) throws Exception {
		try (CsvReporter reporter = new CsvReporter(config, outputDir)) {
			walk(reporter);
		}
	}

	// --- Walk orchestration ---

	private void walkOnce(CommitSink sink) throws Exception {
		RepositoryWalkerUtils.prepareRepository(config.url(), config.gitDir());
		var repoBuilder = new FileRepositoryBuilder().setGitDir(config.gitDir().toFile()).readEnvironment();
		try (org.eclipse.jgit.lib.Repository repo = repoBuilder.build();
		     Git git = new Git(repo);
		     RevWalk rw = new RevWalk(repo)) {
			List<RevCommit> chain = RepositoryWalkerUtils.firstParentChain(repo, rw);
			Map<String, List<String>> tagsByCommit = RepositoryWalkerUtils.tagsByCommit(repo);
			String branch = RepositoryWalkerUtils.defaultBranchName(repo);
			Path workTree = repo.getWorkTree().toPath();
			LOGGER.info("Walking {} commits", chain.size());

			var jdtExtractor = new JdtTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
			var incrementalExtractor = new IncrementalJdtTypesExtractor(jdtExtractor);

			API previousApi = null;
			Path previousSourceRoot = null;
			for (RevCommit revCommit : chain) {
				CommitInfo info = buildCommitInfo(revCommit, repo, tagsByCommit, branch);

				if (!info.javaChanged()) {
					if (previousApi != null) {
						sink.accept(unchangedAnalysis(info, previousApi));
					} else {
						LOGGER.info("Skipping commit {} (no Java changes, no prior API)", info.sha());
					}
					continue;
				}

				long checkoutTime = checkoutCommit(git, revCommit);
				Optional<Path> sourceRoot = resolveSourceRoot();
				if (sourceRoot.isEmpty()) {
					LOGGER.info("Skipping commit {} (no configured source root exists)", info.sha());
					continue;
				}
				LOGGER.info("Commit {}: {} (source root {})", info.sha(), info.shortMessage(), sourceRoot.get());

				ApiResult apiResult = buildApi(info, sourceRoot.get(), previousApi, previousSourceRoot,
					workTree, jdtExtractor, incrementalExtractor);
				DiffResult diffResult = diffApis(previousApi, apiResult.api());

				sink.accept(new CommitAnalysis(
					info, Optional.of(apiResult.api()), diffResult.report(), diffResult.apiChanged(),
					checkoutTime, apiResult.timeMs(), diffResult.timeMs(), apiResult.errors()));

				previousApi = apiResult.api();
				previousSourceRoot = sourceRoot.get();
			}
		}
	}

	// --- Commit info ---

	private CommitInfo buildCommitInfo(RevCommit commit, org.eclipse.jgit.lib.Repository repo,
	                                   Map<String, List<String>> tagsByCommit, String branch) {
		RepositoryWalkerUtils.CommitDiff diff = RepositoryWalkerUtils.computeCommitDiff(repo, commit);
		return new CommitInfo(
			commit.getName(),
			commit.getShortMessage(),
			Instant.ofEpochSecond(commit.getCommitTime()),
			commit.getParentCount() > 1,
			RepositoryWalkerUtils.parentCommit(commit),
			tagsByCommit.getOrDefault(commit.getName(), List.of()),
			branch,
			diff.javaChanged(),
			diff.pomChanged(),
			diff.filesChanged(),
			diff.locAdded(),
			diff.locDeleted(),
			diff.updatedJavaFiles(),
			diff.deletedJavaFiles(),
			diff.createdJavaFiles()
		);
	}

	private static CommitAnalysis unchangedAnalysis(CommitInfo info, API previousApi) {
		LOGGER.info("Commit {} has no Java changes, reusing previous API", info.sha());
		return new CommitAnalysis(info, Optional.of(previousApi), Optional.empty(), false, 0, 0, 0, List.of());
	}

	// --- Checkout ---

	private long checkoutCommit(Git git, RevCommit commit) throws Exception {
		Stopwatch sw = Stopwatch.createStarted();
		RepositoryWalkerUtils.makePristine(git);
		LOGGER.debug("Checking out commit {}", commit.getName());
		git.checkout().setName(commit.getName()).setForced(true).call();
		return sw.elapsed().toMillis();
	}

	private Optional<Path> resolveSourceRoot() {
		return config.sourceRoots().stream().filter(Files::exists).findFirst();
	}

	// --- API building ---

	private record ApiResult(API api, long timeMs, List<Exception> errors) {
	}

	private ApiResult buildApi(CommitInfo info, Path sourceRoot, API previousApi, Path previousSourceRoot,
	                           Path workTree, JdtTypesExtractor extractor,
	                           IncrementalTypesExtractor incrementalExtractor) {
		Stopwatch sw = Stopwatch.createStarted();

		if (!canIncrementalUpdate(previousApi, previousSourceRoot, sourceRoot)) {
			return new ApiResult(fullBuild(sourceRoot, extractor), sw.elapsed().toMillis(), List.of());
		}

		if (isDiffUnknown(info)) {
			var e = new IllegalStateException(
				"Could not compute changed Java files for commit " + info.sha() + "; falling back to full rebuild");
			LOGGER.warn(e.getMessage());
			return new ApiResult(fullBuild(sourceRoot, extractor), sw.elapsed().toMillis(), List.of(e));
		}

		return incrementalBuild(info, sourceRoot, previousApi, workTree, extractor, incrementalExtractor, sw);
	}

	private ApiResult incrementalBuild(CommitInfo info, Path sourceRoot, API previousApi, Path workTree,
	                                   JdtTypesExtractor extractor,
	                                   IncrementalTypesExtractor incrementalExtractor, Stopwatch sw) {
		List<Exception> errors = new ArrayList<>();

		Optional<Path> relativeRoot = RepositoryWalkerUtils.sourceRootRelativeToWorkTree(workTree, sourceRoot);
		if (relativeRoot.isEmpty()) {
			var e = new IllegalStateException(
				"Source root " + sourceRoot + " is outside repository root " + workTree + "; falling back to full rebuild");
			LOGGER.warn(e.getMessage());
			errors.add(e);
			return new ApiResult(fullBuild(sourceRoot, extractor), sw.elapsed().toMillis(), errors);
		}

		ChangedFiles changedFiles = RepositoryWalkerUtils.changedFilesForSourceRoot(info, relativeRoot.get());
		if (changedFiles.hasNoChanges()) {
			return new ApiResult(previousApi, sw.elapsed().toMillis(), errors);
		}

		try {
			LibraryTypes updatedTypes = incrementalExtractor.incrementalUpdate(
				previousApi.getLibraryTypes(), buildLibrary(sourceRoot), changedFiles);
			return new ApiResult(Roseau.buildAPI(updatedTypes), sw.elapsed().toMillis(), errors);
		} catch (RuntimeException e) {
			LOGGER.warn("Incremental update failed for commit {}; falling back to full rebuild", info.sha(), e);
			errors.add(e);
			return new ApiResult(fullBuild(sourceRoot, extractor), sw.elapsed().toMillis(), errors);
		}
	}

	private API fullBuild(Path sourceRoot, JdtTypesExtractor extractor) {
		return Roseau.buildAPI(extractor.extractTypes(buildLibrary(sourceRoot)));
	}

	private Library buildLibrary(Path sourceRoot) {
		return Library.builder()
			.location(sourceRoot)
			.classpath(List.of())
			.exclusions(config.exclusions())
			.build();
	}

	private static boolean canIncrementalUpdate(API previousApi, Path previousSourceRoot, Path sourceRoot) {
		return previousApi != null && previousSourceRoot != null && previousSourceRoot.equals(sourceRoot);
	}

	private static boolean isDiffUnknown(CommitInfo info) {
		return info.updatedJavaFiles().isEmpty()
			&& info.deletedJavaFiles().isEmpty()
			&& info.createdJavaFiles().isEmpty();
	}

	// --- API diffing ---

	private record DiffResult(Optional<RoseauReport> report, boolean apiChanged, long timeMs) {
	}

	private static DiffResult diffApis(API previousApi, API currentApi) {
		if (previousApi == null || currentApi == previousApi || currentApi.equals(previousApi)) {
			return new DiffResult(Optional.empty(), false, 0);
		}
		Stopwatch sw = Stopwatch.createStarted();
		RoseauReport report = Roseau.diff(previousApi, currentApi);
		LOGGER.info("Found {} breaking changes", report.getAllBreakingChanges().size());
		return new DiffResult(Optional.of(report), true, sw.elapsed().toMillis());
	}
}
