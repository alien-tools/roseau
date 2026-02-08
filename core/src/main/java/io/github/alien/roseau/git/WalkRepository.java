package io.github.alien.roseau.git;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauOptions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
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

/**
 * Helpful resources when setting up a repository for analysis:
 * - git log --reverse --format="%H %cd %s" -- src/main/java | head -n 1
 * - git log --reverse --format="%H %cd %s" -- pom.xml | head -n 1
 */
public class WalkRepository {
	private static final Logger LOGGER = LogManager.getLogger(WalkRepository.class);

	static void main() throws Exception {
		Path config = Path.of("walk.yaml");
		List<RepositoryWalkerUtils.Repository> repos = RepositoryWalkerUtils.loadConfig(config);
		repos.parallelStream().forEach(repo -> {
			try {
				walk(repo.id(), repo.url(), repo.gitDir(), repo.sourceRoots(), List.of(), repo.outputDir(), repo.exclusions());
			} catch (Exception e) {
				LOGGER.error("Analysis of {} failed", repo.url(), e);
			}
		});
	}

	static void walk(String library, String url, Path gitDir, List<Path> sources, List<Path> poms, Path outputDir,
	                 RoseauOptions.Exclude exclusions) throws Exception {
		try {
			walkOnce(library, url, gitDir, sources, poms, outputDir, exclusions);
		} catch (Exception e) {
			if (!RepositoryWalkerUtils.isMissingObjectFailure(e)) {
				throw e;
			}
			LOGGER.warn("Repository {} is missing objects during checkout, re-cloning and retrying once", gitDir, e);
			RepositoryWalkerUtils.recloneRepository(url, gitDir);
			walkOnce(library, url, gitDir, sources, poms, outputDir, exclusions);
		}
	}

	private static void walkOnce(String library, String url, Path gitDir, List<Path> sources, List<Path> poms, Path outputDir,
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
			LOGGER.info("Walking {} commits", chain.size());

			API oldApi = null;
			RepositoryWalkerUtils.ApiStats oldStats = null;
			RepositoryWalkerUtils.ExclusionMatcher exclusionMatcher =
				RepositoryWalkerUtils.exclusionMatcher(exclusions);
			Map<String, List<String>> tagsByCommit = RepositoryWalkerUtils.tagsByCommit(repo);
			String branch = RepositoryWalkerUtils.defaultBranchName(repo);
			RevCommit previousWrittenCommit = null;
			List<Path> classpath = List.of();
			for (RevCommit commit : chain) {
				String sha = commit.getName();
				String msg = commit.getShortMessage();
				String conventionalCommitTag = RepositoryWalkerUtils.conventionalCommitTag(msg);
				String parentCommit = RepositoryWalkerUtils.parentCommit(commit);
				String tags = RepositoryWalkerUtils.joinedTags(tagsByCommit, sha);
				String version = RepositoryWalkerUtils.resolveVersionFromTags(tagsByCommit, sha);

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
							version,
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

				LOGGER.info("Commit {}: {}", sha, msg);

				if (sources.stream().noneMatch(Files::exists)/* || poms.stream().noneMatch(Files::exists)*/) {
					LOGGER.info("Skipping.");
					continue;
				}

				long classpathTime = 0L;

				Path src = sources.stream().filter(Files::exists).findFirst().orElseThrow();
				sw.reset().start();
				API currentApi = RepositoryWalkerUtils.buildApi(src, classpath, exclusions);
				long apiTime = sw.elapsed().toMillis();
				sw.reset().start();
				RepositoryWalkerUtils.ApiStats currentStats = RepositoryWalkerUtils.computeApiStats(currentApi, exclusionMatcher);
				long statsTime = sw.elapsed().toMillis();

				List<BreakingChange> bcs = List.of();
				long diffTime = 0;
				if (oldApi != null) {
					sw.reset().start();
					RoseauReport diff = Roseau.diff(oldApi, currentApi);
					diffTime = sw.elapsed().toMillis();
					bcs = diff.getAllBreakingChanges();
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
					version,
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
			}
		}
	}
}
