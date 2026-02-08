package io.github.alien.roseau.git;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauOptions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import io.github.alien.roseau.extractors.incremental.IncrementalTypesExtractor;
import io.github.alien.roseau.extractors.jdt.IncrementalJdtTypesExtractor;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Incremental commit walker: builds APIs incrementally from one commit to the next.
 */
public class IncrementalWalkRepository {
	private static final Logger LOGGER = LogManager.getLogger(IncrementalWalkRepository.class);

	static void main() throws Exception {
		Path config = Path.of("walk.yaml");
		List<RepositoryWalkerUtils.Repository> repos = RepositoryWalkerUtils.loadConfig(config);
		repos.parallelStream().forEach(repo -> {
			try {
				walk(repo.id(), repo.url(), repo.gitDir(), repo.sourceRoots(), repo.outputDir(), repo.exclusions());
			} catch (Exception e) {
				LOGGER.error("Incremental analysis of {} failed", repo.url(), e);
			}
		});
	}

	static void walk(String library, String url, Path gitDir, List<Path> sourceRoots, Path outputDir,
	                 RoseauOptions.Exclude exclusions) throws Exception {
		try {
			walkOnce(library, url, gitDir, sourceRoots, outputDir, exclusions);
		} catch (Exception e) {
			if (!RepositoryWalkerUtils.isMissingObjectFailure(e)) {
				throw e;
			}
			LOGGER.warn("Repository {} is missing objects during checkout, re-cloning and retrying once", gitDir, e);
			RepositoryWalkerUtils.recloneRepository(url, gitDir);
			walkOnce(library, url, gitDir, sourceRoots, outputDir, exclusions);
		}
	}

	private static void walkOnce(String library, String url, Path gitDir, List<Path> sourceRoots, Path outputDir,
	                             RoseauOptions.Exclude exclusions) throws Exception {
		RepositoryWalkerUtils.prepareRepository(url, gitDir);
		Stopwatch sw = Stopwatch.createUnstarted();
		FileRepositoryBuilder builder = new FileRepositoryBuilder().setGitDir(gitDir.toFile()).readEnvironment();
		RepositoryWalkerUtils.OutputFiles outputFiles = RepositoryWalkerUtils.resolveOutputFiles(library, outputDir);
		try (BufferedWriter commitsWriter = RepositoryWalkerUtils.openCsvWriter(outputFiles.commitsCsv());
		     BufferedWriter bcsWriter = RepositoryWalkerUtils.openCsvWriter(outputFiles.bcsCsv());
		     org.eclipse.jgit.lib.Repository repo = builder.build();
		     Git git = new Git(repo);
		     RevWalk rw = new RevWalk(repo)) {
			LOGGER.info("Writing commit data to {}", outputFiles.commitsCsv().toAbsolutePath().normalize());
			LOGGER.info("Writing breaking changes data to {}", outputFiles.bcsCsv().toAbsolutePath().normalize());
			RepositoryWalkerUtils.writeCsvHeader(commitsWriter, RepositoryWalkerUtils.COMMITS_HEADER);
			RepositoryWalkerUtils.writeCsvHeader(bcsWriter, RepositoryWalkerUtils.BCS_HEADER);

			List<RevCommit> chain = RepositoryWalkerUtils.firstParentChain(repo, rw);
			LOGGER.info("Incrementally walking {} commits", chain.size());

			API oldApi = null;
			RepositoryWalkerUtils.ApiStats oldStats = null;
			RepositoryWalkerUtils.ExclusionMatcher exclusionMatcher =
				RepositoryWalkerUtils.exclusionMatcher(exclusions);
			Path oldSourceRoot = null;
			RevCommit previousWrittenCommit = null;
			Map<String, List<String>> tagsByCommit = RepositoryWalkerUtils.tagsByCommit(repo);
			String branch = RepositoryWalkerUtils.defaultBranchName(repo);
			List<Path> classpath = List.of();

			JdtTypesExtractor jdtExtractor = new JdtTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
			IncrementalTypesExtractor incrementalExtractor = new IncrementalJdtTypesExtractor(jdtExtractor);

			for (RevCommit commit : chain) {
				String sha = commit.getName();
				String msg = commit.getShortMessage();
				String conventionalCommitTag = RepositoryWalkerUtils.conventionalCommitTag(msg);
				String parentCommit = RepositoryWalkerUtils.parentCommit(commit);
				String tags = RepositoryWalkerUtils.joinedTags(tagsByCommit, sha);

				RepositoryWalkerUtils.CommitDiff commitDiff = RepositoryWalkerUtils.computeCommitDiff(repo, commit);
				if (!commitDiff.javaChanged()) {
					if (oldStats != null) {
						long daysSincePrevCommit = RepositoryWalkerUtils.daysSincePreviousCommit(previousWrittenCommit, commit);
						RepositoryWalkerUtils.writeCommitRow(
							commitsWriter,
							library,
							url,
							commit,
							conventionalCommitTag,
							parentCommit,
							branch,
							tags,
							tags,
							daysSincePrevCommit,
							new RepositoryWalkerUtils.CommitAnalysis(commitDiff, oldStats, 0, 0, 0, 0, 0, 0, 0, 0)
						);
						previousWrittenCommit = commit;
						LOGGER.info("Skipping commit {} (no Java source changes), reusing previous API stats", sha);
					} else {
						LOGGER.info("Skipping commit {} (no Java source changes), no previous API stats to reuse", sha);
					}
					continue;
				}

				sw.reset().start();
				RepositoryWalkerUtils.makePristine(git);
				LOGGER.debug("Checking out commit {}", sha);
				git.checkout()
					.setName(sha)
					.setForced(true)
					.call();
				long checkoutTime = sw.elapsed().toMillis();

				Optional<Path> srcOpt = sourceRoots.stream().filter(Files::exists).findFirst();
				if (srcOpt.isEmpty()) {
					LOGGER.info("Skipping commit {} (no configured source root exists)", sha);
					continue;
				}
				Path sourceRoot = srcOpt.get();
				LOGGER.info("Commit {}: {} (source root {})", sha, msg, sourceRoot);

				long classpathTime = 0L;
				boolean canIncremental = oldApi != null && oldSourceRoot != null && oldSourceRoot.equals(sourceRoot);
				boolean diffUnknown = commitDiff.updatedJavaFiles().isEmpty()
					&& commitDiff.deletedJavaFiles().isEmpty()
					&& commitDiff.createdJavaFiles().isEmpty();

				sw.reset().start();
				API currentApi;
				if (!canIncremental || diffUnknown) {
					if (diffUnknown) {
						LOGGER.warn("Could not compute changed Java files for commit {}; falling back to full rebuild", sha);
					}
					currentApi = RepositoryWalkerUtils.buildApi(sourceRoot, classpath, exclusions);
				} else {
					Optional<Path> sourceRootRelative = RepositoryWalkerUtils.sourceRootRelativeToWorkTree(
						repo.getWorkTree().toPath(), sourceRoot);
					if (sourceRootRelative.isEmpty()) {
						LOGGER.warn("Source root {} is outside repository root {}; falling back to full rebuild",
							sourceRoot, repo.getWorkTree());
						currentApi = RepositoryWalkerUtils.buildApi(sourceRoot, classpath, exclusions);
					} else {
						ChangedFiles changedFiles = RepositoryWalkerUtils.changedFilesForSourceRoot(commitDiff, sourceRootRelative.get());
						if (changedFiles.hasNoChanges()) {
							currentApi = oldApi;
						} else {
							try {
								Library currentLibrary = Library.builder()
									.location(sourceRoot)
									.classpath(classpath)
									.exclusions(exclusions)
									.build();
								LibraryTypes updatedTypes = incrementalExtractor.incrementalUpdate(
									oldApi.getLibraryTypes(), currentLibrary, changedFiles);
								currentApi = updatedTypes.toAPI();
							} catch (RuntimeException e) {
								LOGGER.warn("Incremental update failed for commit {}; falling back to full rebuild", sha, e);
								currentApi = RepositoryWalkerUtils.buildApi(sourceRoot, classpath, exclusions);
							}
						}
					}
				}
				long apiTime = sw.elapsed().toMillis();

				RepositoryWalkerUtils.ApiStats currentStats;
				long statsTime;
				if (currentApi == oldApi && oldStats != null) {
					currentStats = oldStats;
					statsTime = 0;
				} else {
					sw.reset().start();
					currentStats = RepositoryWalkerUtils.computeApiStats(currentApi, exclusionMatcher);
					statsTime = sw.elapsed().toMillis();
				}

				long diffTime;
				List<BreakingChange> bcs;
				if (oldApi == null || currentApi == oldApi) {
					diffTime = 0;
					bcs = List.of();
				} else {
					sw.reset().start();
					RoseauReport diff = Roseau.diff(oldApi, currentApi);
					diffTime = sw.elapsed().toMillis();
					bcs = diff.getAllBreakingChanges();
				}
				if (oldApi != null) {
					LOGGER.info("Found {} breaking changes", bcs.size());
					RepositoryWalkerUtils.writeBreakingChangesRows(bcsWriter, library, sha, oldApi, bcs, exclusionMatcher);
				}
				int binaryBreakingChangesCount = (int) bcs.stream().map(BreakingChange::kind).filter(BreakingChangeKind::isBinaryBreaking).count();
				int sourceBreakingChangesCount = (int) bcs.stream().map(BreakingChange::kind).filter(BreakingChangeKind::isSourceBreaking).count();
				long daysSincePrevCommit = RepositoryWalkerUtils.daysSincePreviousCommit(previousWrittenCommit, commit);
				RepositoryWalkerUtils.writeCommitRow(
					commitsWriter,
					library,
					url,
					commit,
					conventionalCommitTag,
					parentCommit,
					branch,
					tags,
					tags,
					daysSincePrevCommit,
					new RepositoryWalkerUtils.CommitAnalysis(
						commitDiff,
						currentStats,
						bcs.size(),
						binaryBreakingChangesCount,
						sourceBreakingChangesCount,
						checkoutTime,
						classpathTime,
						apiTime,
						diffTime,
						statsTime
					)
				);
				previousWrittenCommit = commit;

				oldApi = currentApi;
				oldStats = currentStats;
				oldSourceRoot = sourceRoot;
			}
		}
	}
}
