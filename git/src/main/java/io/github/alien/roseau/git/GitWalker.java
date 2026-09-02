package io.github.alien.roseau.git;

import com.google.common.base.Preconditions;
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
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Walks a Git repository's first-parent commit chain, building APIs incrementally and
 * computing breaking changes between consecutive commits. Results are emitted to a
 * {@link CommitSink} for each processed commit.
 */
public final class GitWalker {
	private static final Logger LOGGER = LogManager.getLogger(GitWalker.class);

	/** Upper bound on any native {@code git} invocation, so a hung command cannot stall a whole walk. */
	private static final Duration COMMAND_TIMEOUT = Duration.ofMinutes(10);

	public record Config(
		String libraryId,
		String url,
		Path gitDir,
		List<Path> sourceRoots,
		RoseauOptions.Exclude exclusions,
		String startSha,
		String endSha
	) {
		public Config {
			Preconditions.checkNotNull(libraryId, "libraryId must be provided");
			Preconditions.checkNotNull(url, "url must be provided");
			Preconditions.checkNotNull(gitDir, "gitDir must be provided");
			Preconditions.checkNotNull(sourceRoots, "sourceRoots must be provided");
			Preconditions.checkNotNull(startSha, "startSha must be provided; use ROOT_COMMIT to walk the whole history");
			Preconditions.checkNotNull(endSha, "endSha must be provided; use HEAD to walk up to the current tip");
			// Exclusions are the one genuinely optional part of a configuration
			exclusions = exclusions == null ? NO_EXCLUSIONS : exclusions;
			sourceRoots = List.copyOf(sourceRoots);
		}
	}

	/**
	 * An empty set of exclusions, applied when a configuration does not define any.
	 */
	public static final RoseauOptions.Exclude NO_EXCLUSIONS = new RoseauOptions.Exclude(List.of(), List.of());

	/**
	 * {@link Config#startSha()} value walking back to the repository's root commit.
	 */
	public static final String ROOT_COMMIT = "";

	/**
	 * {@link Config#endSha()} value walking from whatever {@code HEAD} currently points at. Prefer an explicit commit:
	 * {@code HEAD} moves whenever the repository is fetched, making the walk irreproducible.
	 */
	public static final String HEAD = "HEAD";

	private final Config config;

	public GitWalker(Config config) {
		this.config = config;
	}

	/**
	 * Analyzes a Git repository's first-parent commit chain, processes API changes, and delivers
	 * the analysis results through the provided {@code CommitSink}.
	 *
	 * @param sink the recipient of the {@link CommitAnalysis} results for the processed commits
	 * @throws Exception if any error occurs
	 */
	public void walk(CommitSink sink) throws Exception {
		prepareRepository(config.url(), config.gitDir());
		FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder().setGitDir(
			config.gitDir().toFile()).readEnvironment();
		try (Repository repo = repoBuilder.build();
		     Git git = new Git(repo);
		     RevWalk rw = new RevWalk(repo)) {
			List<RevCommit> chain = firstParentChain(repo, rw, config.startSha(), config.endSha());
			Map<String, List<String>> tagsByCommit = tagsByCommit(repo);
			String branch = defaultBranchName(repo);
			Path workTree = repo.getWorkTree().toPath();
			LOGGER.info("Walking {} commits", chain.size());

			JdtTypesExtractor jdtExtractor = new JdtTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
			IncrementalTypesExtractor incrementalExtractor = new IncrementalJdtTypesExtractor(jdtExtractor);

			API previousApi = null;
			Path previousSourceRoot = null;
			Optional<Path> previousRelativeRoot = Optional.empty();
			for (RevCommit revCommit : chain) {
				CommitDiff diff = buildCommitDiff(repo, revCommit, rw);
				CommitInfo info = buildCommitInfo(diff, revCommit, tagsByCommit, branch);

				// FIXME: only correct because we don't care about classpath yet
				if (!info.javaChanged()) {
					if (previousApi != null) {
						sink.accept(unchangedAnalysis(info, previousApi, previousRelativeRoot));
					} else {
						sink.accept(emptyAnalysis(info));
					}
					continue;
				}

				long checkoutTime = checkoutCommit(git, workTree, revCommit);
				Optional<Path> sourceRoot = resolveSourceRoot();
				if (sourceRoot.isEmpty()) {
					LOGGER.info("No configured source root exists at commit {}; reporting it without an API",
						info.sha());
					sink.accept(noSourceRootAnalysis(info, previousApi, checkoutTime));
					continue;
				}
				LOGGER.info("Commit {}: {} (source root {})", info.sha(), info.shortMessage(), sourceRoot.get());

				ApiResult apiResult = buildApi(info, diff, sourceRoot.get(), previousApi, previousSourceRoot,
					workTree, jdtExtractor, incrementalExtractor);
				DiffResult diffResult = diffApis(previousApi, apiResult.api());

				sink.accept(new CommitAnalysis(
					info, Optional.of(apiResult.api()), diffResult.report(), diffResult.apiChanged(),
					checkoutTime, apiResult.timeMs(), diffResult.timeMs(),
					concatErrors(apiResult.errors(), diff.error()),
					Optional.of(workTree.relativize(sourceRoot.get()))));

				previousApi = apiResult.api();
				previousSourceRoot = sourceRoot.get();
				previousRelativeRoot = Optional.of(workTree.relativize(sourceRoot.get()));
			}
		}
	}

	private static CommitInfo buildCommitInfo(CommitDiff diff, RevCommit commit,
	                                          Map<String, List<String>> tagsByCommit, String branch) {
		return new CommitInfo(
			commit.getName(),
			commit.getShortMessage(),
			Instant.ofEpochSecond(commit.getCommitTime()),
			commit.getParentCount() > 1,
			parentCommit(commit),
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

	private static CommitAnalysis unchangedAnalysis(CommitInfo info, API previousApi, Optional<Path> sourceRoot) {
		return new CommitAnalysis(info, Optional.of(previousApi), Optional.empty(), false, 0, 0, 0, List.of(),
			sourceRoot);
	}

	private static CommitAnalysis emptyAnalysis(CommitInfo info) {
		return new CommitAnalysis(info, Optional.empty(), Optional.empty(), false, 0, 0, 0, List.of(),
			Optional.empty());
	}

	private static CommitAnalysis noSourceRootAnalysis(CommitInfo info, API previousApi, long checkoutTime) {
		return new CommitAnalysis(info, Optional.ofNullable(previousApi), Optional.empty(), false,
			checkoutTime, 0, 0, List.of(), Optional.empty());
	}

	private static List<String> concatErrors(List<String> errors, String diffError) {
		return diffError == null
			? errors
			: Stream.concat(errors.stream(), Stream.of(diffError)).toList();
	}

	private static long checkoutCommit(Git git, Path workTree, RevCommit commit) throws Exception {
		Stopwatch sw = Stopwatch.createStarted();
		makePristine(git);
		try {
			git.checkout().setName(commit.getName()).setForced(true).call();
		} catch (JGitInternalException e) {
			if (!isMissingObjectCheckoutFailure(e)) {
				throw e;
			}
			LOGGER.warn("JGit checkout failed for commit {}; falling back to native git", commit.getName(), e);
			makePristine(workTree);
			runGit(workTree, "checkout", "--force", commit.getName());
		}
		return sw.elapsed().toMillis();
	}

	private Optional<Path> resolveSourceRoot() {
		return config.sourceRoots().stream().filter(Files::exists).findFirst();
	}

	private record ApiResult(API api, long timeMs, List<String> errors) {
	}

	private ApiResult buildApi(CommitInfo info, CommitDiff diff, Path sourceRoot,
	                           API previousApi, Path previousSourceRoot,
	                           Path workTree, JdtTypesExtractor extractor,
	                           IncrementalTypesExtractor incrementalExtractor) {
		return canIncrementalUpdate(previousApi, previousSourceRoot, sourceRoot)
			? buildApiIncremental(info, diff, sourceRoot, previousApi, workTree, extractor, incrementalExtractor)
			: buildApiFull(sourceRoot, extractor);
	}

	private ApiResult buildApiFull(Path sourceRoot, JdtTypesExtractor extractor) {
		Library library = buildLibrary(sourceRoot);
		Stopwatch sw = Stopwatch.createStarted();
		API api = Roseau.buildAPI(extractor.extractTypes(library));
		return new ApiResult(api, sw.elapsed().toMillis(), List.of());
	}

	private ApiResult buildApiIncremental(CommitInfo info, CommitDiff diff,
	                                      Path sourceRoot, API previousApi, Path workTree,
	                                      JdtTypesExtractor extractor,
	                                      IncrementalTypesExtractor incrementalExtractor) {
		Path relativeRoot = workTree.relativize(sourceRoot);
		Stopwatch sw = Stopwatch.createStarted();
		ChangedFiles changedFiles = changedFilesForSourceRoot(diff, relativeRoot);
		if (changedFiles.hasNoChanges()) {
			return new ApiResult(previousApi, sw.elapsed().toMillis(), List.of());
		}

		try {
			LibraryTypes updatedTypes = incrementalExtractor.incrementalUpdate(
				previousApi.getLibraryTypes(), buildLibrary(sourceRoot), changedFiles);
			return new ApiResult(Roseau.buildAPI(updatedTypes), sw.elapsed().toMillis(), List.of());
		} catch (RuntimeException e) {
			LOGGER.warn("Incremental update failed for commit {}; falling back to full rebuild", info.sha(), e);
			return new ApiResult(buildApiFull(sourceRoot, extractor).api(), sw.elapsed().toMillis(),
				List.of("incremental update failed, rebuilt from scratch: " + e.getMessage()));
		}
	}

	private Library buildLibrary(Path sourceRoot) {
		// Exclusions are deliberately not applied here: we want breaking changes to be detected on excluded symbols
		// too, and filtered downstream, so that public and internal breakage can be reported separately.
		return Library.builder()
			.location(sourceRoot)
			.classpath(List.of())
			.build();
	}

	private static boolean canIncrementalUpdate(API previousApi, Path previousSourceRoot, Path sourceRoot) {
		return previousApi != null && previousSourceRoot != null && previousSourceRoot.equals(sourceRoot);
	}

	private record DiffResult(Optional<RoseauReport> report, boolean apiChanged, long timeMs) {
	}

	private static DiffResult diffApis(API previousApi, API currentApi) {
		if (previousApi == null || currentApi == previousApi || currentApi.equals(previousApi)) {
			return new DiffResult(Optional.empty(), false, 0);
		}
		Stopwatch sw = Stopwatch.createStarted();
		RoseauReport report = Roseau.diff(previousApi, currentApi);
		return new DiffResult(Optional.of(report), true, sw.elapsed().toMillis());
	}

	record CommitDiff(
		boolean javaChanged,
		boolean pomChanged,
		int filesChanged,
		int locAdded,
		int locDeleted,
		Set<Path> updatedJavaFiles,
		Set<Path> deletedJavaFiles,
		Set<Path> createdJavaFiles,
		String error
	) {
	}

	public static void prepareRepository(String url, Path gitDir) throws Exception {
		if (!Files.exists(gitDir)) {
			Path workTree = gitDir.getParent();
			LOGGER.info("Local clone not found for {}, cloning into {}", url, workTree);
			cloneRepository(url, gitDir.getParent());
		}

		LOGGER.info("Preparing existing clone at {} (remote {})", gitDir, url);
		FileRepositoryBuilder builder = new FileRepositoryBuilder()
			.setGitDir(gitDir.toFile())
			.readEnvironment();
		try (Repository repo = builder.build(); Git git = new Git(repo)) {
			fetchDefaultBranch(git, repo);
			makePristine(git);
		}
	}

	/**
	 * Collects the first-parent chain in chronological order, from {@code startSha} (or the root commit when it is
	 * {@link #ROOT_COMMIT}) to {@code endSha} (or the current tip when it is {@link #HEAD}).
	 *
	 * @throws IllegalStateException if {@code endSha} cannot be resolved, or if {@code startSha} is not on the
	 *                               first-parent chain reaching {@code endSha}
	 */
	static List<RevCommit> firstParentChain(Repository repo, RevWalk rw, String startSha, String endSha)
		throws IOException {
		String tip = endSha.isBlank() ? HEAD : endSha;
		ObjectId tipId = repo.resolve(tip);
		if (tipId == null) {
			throw new IllegalStateException("Cannot resolve %s in %s".formatted(tip, repo.getDirectory()));
		}

		boolean walkToRoot = startSha.isBlank();
		List<RevCommit> chain = new ArrayList<>();
		RevCommit cur = rw.parseCommit(tipId);
		while (true) {
			chain.add(cur);
			if (cur.getParentCount() == 0 || (!walkToRoot && cur.name().equals(startSha))) {
				break;
			}
			cur = rw.parseCommit(cur.getParent(0));
		}

		// Reaching the root commit without having seen startSha means it is not an ancestor of endSha along
		// first-parent links; silently analysing a different range would be worse than failing.
		if (!walkToRoot && !cur.name().equals(startSha)) {
			throw new IllegalStateException("startSha %s is not on the first-parent chain of %s in %s"
				.formatted(startSha, tip, repo.getDirectory()));
		}

		Collections.reverse(chain);
		return chain;
	}

	static CommitDiff buildCommitDiff(Repository repo, RevCommit commit, RevWalk rw) {
		try (ObjectReader reader = repo.newObjectReader();
		     DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
			df.setRepository(repo);
			df.setDetectRenames(false);

			CanonicalTreeParser newTree = new CanonicalTreeParser();
			newTree.reset(reader, commit.getTree().getId());

			AbstractTreeIterator oldTree;
			if (commit.getParentCount() > 0) {
				CanonicalTreeParser parentTree = new CanonicalTreeParser();
				RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
				parentTree.reset(reader, parent.getTree().getId());
				oldTree = parentTree;
			} else {
				oldTree = new EmptyTreeIterator();
			}

			List<DiffEntry> diffs = df.scan(oldTree, newTree);

			Set<Path> updated = new HashSet<>();
			Set<Path> deleted = new HashSet<>();
			Set<Path> created = new HashSet<>();
			boolean pomChanged = false;
			int locAdded = 0;
			int locDeleted = 0;

			for (DiffEntry e : diffs) {
				Path oldPath = pathOf(e.getOldPath());
				Path newPath = pathOf(e.getNewPath());

				if (isPomPath(oldPath) || isPomPath(newPath)) {
					pomChanged = true;
				}

				switch (e.getChangeType()) {
					case ADD -> addIfJava(created, newPath);
					case MODIFY -> addIfJava(updated, newPath);
					case DELETE -> addIfJava(deleted, oldPath);
					case RENAME -> {
						addIfJava(deleted, oldPath);
						addIfJava(created, newPath);
					}
					case COPY -> addIfJava(created, newPath);
				}

				for (Edit edit : df.toFileHeader(e).toEditList()) {
					locAdded += edit.getLengthB();
					locDeleted += edit.getLengthA();
				}
			}

			boolean javaChanged = !updated.isEmpty() || !deleted.isEmpty() || !created.isEmpty();
			return new CommitDiff(javaChanged, pomChanged, diffs.size(), locAdded, locDeleted, updated, deleted, created,
				null);
		} catch (IOException e) {
			LOGGER.warn("Failed to compute commit diff for {}", commit.getName(), e);
			return new CommitDiff(true, true, 0, 0, 0, Set.of(), Set.of(), Set.of(),
				"commit diff failed: " + e.getMessage());
		}
	}

	static ChangedFiles changedFilesForSourceRoot(CommitDiff diff, Path sourceRootRelative) {
		return new ChangedFiles(
			relativizeUnderRoot(diff.updatedJavaFiles(), sourceRootRelative),
			relativizeUnderRoot(diff.deletedJavaFiles(), sourceRootRelative),
			relativizeUnderRoot(diff.createdJavaFiles(), sourceRootRelative)
		);
	}

	static Map<String, List<String>> tagsByCommit(Repository repo) throws IOException {
		Map<String, List<String>> tagsByCommit = new HashMap<>();
		try (RevWalk rw = new RevWalk(repo)) {
			for (Ref ref : repo.getRefDatabase().getRefsByPrefix(Constants.R_TAGS)) {
				Ref peeled = repo.getRefDatabase().peel(ref);
				ObjectId objectId = peeled.getPeeledObjectId() != null ? peeled.getPeeledObjectId() : ref.getObjectId();
				if (objectId == null) {
					continue;
				}
				try {
					RevCommit commit = rw.parseCommit(objectId);
					String tag = ref.getName().substring(Constants.R_TAGS.length());
					tagsByCommit.computeIfAbsent(commit.getName(), _ -> new ArrayList<>()).add(tag);
				} catch (Exception ignored) {
					// Ignore tags not pointing to commits
				}
			}
		}
		tagsByCommit.values().forEach(Collections::sort);
		return tagsByCommit;
	}

	static String defaultBranchName(Repository repo) throws IOException {
		return resolveRemoteBranchName(repo);
	}

	static String parentCommit(RevCommit commit) {
		return commit.getParentCount() > 0 ? commit.getParent(0).getName() : "";
	}

	private static boolean isMissingObjectCheckoutFailure(JGitInternalException e) {
		return e.getCause() instanceof MissingObjectException;
	}

	static void makePristine(Path workTree) throws Exception {
		if (workTree == null) {
			throw new IOException("Cannot resolve repository work tree");
		}
		runGit(workTree, "reset", "--hard");
		runGit(workTree, "clean", "-fdx");
	}

	static void makePristine(Git git) throws Exception {
		git.reset().setMode(ResetCommand.ResetType.HARD).call();
		git.clean().setCleanDirectories(true).setIgnore(false).call();
	}

	private static void cloneRepository(String url, Path workTree) throws GitAPIException, IOException {
		if (workTree == null) {
			throw new IOException("Cannot resolve repository work tree");
		}
		Path parent = workTree.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		LOGGER.info("Cloning {} into {}", url, workTree);
		Git.cloneRepository().setURI(url).setDirectory(workTree.toFile()).call().close();
	}

	private static void fetchDefaultBranch(Git git, Repository repo) throws Exception {
		LOGGER.info("Fetching updates from origin for {}", repo.getDirectory());
		var fetch = git.fetch().setRemote("origin");
		fetch.call();

		String branchName = resolveRemoteBranchName(repo);
		LOGGER.info("Aligning local repository to origin/{}", branchName);
		if (repo.findRef("refs/heads/" + branchName) == null) {
			git.checkout().setCreateBranch(true).setName(branchName).setStartPoint("origin/" + branchName).call();
		} else {
			git.checkout().setName(branchName).setForced(true).call();
		}
		git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/" + branchName).call();
	}

	private static void runGit(Path workTree, String... args) throws Exception {
		List<String> command = new ArrayList<>(args.length + 4);
		command.add("git");
		command.add("-c");
		command.add("submodule.recurse=false");
		command.add("-C");
		command.add(workTree.toString());
		Collections.addAll(command, args);
		runCommand(command);
	}

	private static void runCommand(List<String> command) throws IOException, InterruptedException {
		Process process = new ProcessBuilder(command)
			.redirectErrorStream(true)
			.start();
		try {
			// stdout and stderr are merged, so draining the single stream before waiting cannot deadlock
			String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			if (!process.waitFor(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
				throw new IOException("Command timed out after %s: %s".formatted(COMMAND_TIMEOUT,
					String.join(" ", command)));
			}
			int exit = process.exitValue();
			if (exit != 0) {
				throw new IOException("Command failed (%d): %s%n%s".formatted(exit, String.join(" ", command), output));
			}
		} finally {
			if (process.isAlive()) {
				process.destroyForcibly();
			}
		}
	}

	private static String resolveRemoteBranchName(Repository repo) throws IOException {
		final String prefix = "refs/remotes/origin/";

		Ref remoteHead = repo.exactRef("refs/remotes/origin/HEAD");
		if (remoteHead != null) {
			String remoteRefName = remoteHead.getTarget().getName();
			if (remoteRefName.startsWith(prefix)) {
				return remoteRefName.substring(prefix.length());
			}
		}

		// Repository#getBranch is @Nullable on a corrupt repository, so it cannot go into List.of
		List<String> candidates = new ArrayList<>(List.of("main", "master"));
		String currentBranch = repo.getBranch();
		if (currentBranch != null) {
			candidates.add(currentBranch);
		}
		for (String candidate : candidates) {
			if (repo.findRef(prefix + candidate) != null) {
				return candidate;
			}
		}

		throw new IllegalStateException("Could not resolve default branch for " + repo.getDirectory());
	}

	private static Set<Path> relativizeUnderRoot(Set<Path> paths, Path sourceRootRelative) {
		return paths.stream()
			.filter(p -> p.startsWith(sourceRootRelative))
			.map(sourceRootRelative::relativize)
			.collect(Collectors.toSet());
	}

	private static void addIfJava(Set<Path> set, Path path) {
		if (isJavaPath(path)) {
			set.add(path);
		}
	}

	private static boolean isJavaPath(Path path) {
		return path != null && path.toString().endsWith(".java");
	}

	private static boolean isPomPath(Path path) {
		if (path == null) {
			return false;
		}
		String str = path.toString();
		return str.equals("pom.xml") || str.endsWith("/pom.xml");
	}

	private static Path pathOf(String path) {
		if (path == null || DiffEntry.DEV_NULL.equals(path)) {
			return null;
		}
		return Path.of(path);
	}
}
