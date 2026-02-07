package io.github.alien.roseau.git;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Date;
import java.util.List;

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
		repos.forEach(repo -> {
			try {
				walk(repo.url(), repo.gitDir(), repo.sourceRoots(), List.of(), repo.csv());
			} catch (Exception e) {
				LOGGER.error("Analysis of {} failed", repo.url(), e);
			}
		});
	}

	static void walk(String url, Path gitDir, List<Path> sources, List<Path> poms, Path csv) throws Exception {
		RepositoryWalkerUtils.prepareRepository(url, gitDir);
		Stopwatch sw = Stopwatch.createUnstarted();
		FileRepositoryBuilder builder = new FileRepositoryBuilder().setGitDir(gitDir.toFile()).readEnvironment();
		try (BufferedWriter csvWriter = Files.newBufferedWriter(csv,
			StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		     org.eclipse.jgit.lib.Repository repo = builder.build();
		     Git git = new Git(repo);
		     RevWalk rw = new RevWalk(repo)) {
			csvWriter.write(RepositoryWalkerUtils.HEADER);

			List<RevCommit> chain = RepositoryWalkerUtils.firstParentChain(repo, rw);
			LOGGER.info("Walking {} commits", chain.size());

			API oldApi = null;
			RepositoryWalkerUtils.ApiStats oldStats = null;
			List<Path> classpath = List.of();
			for (RevCommit commit : chain) {
				String sha = commit.getName();
				Date date = Date.from(Instant.ofEpochSecond(commit.getCommitTime()));
				String msg = commit.getShortMessage();

				RepositoryWalkerUtils.Flags flags = RepositoryWalkerUtils.changedJavaOrPom(repo, commit);
				if (!flags.javaChanged()) {
					if (oldStats != null) {
						csvWriter.write(RepositoryWalkerUtils.csvLine(
							sha, date, msg, url, oldStats,
							0, 0, 0, 0, 0, 0, ""));
						LOGGER.info("Skipping commit {} (no Java source changes), reusing previous API stats", sha);
					} else {
						LOGGER.info("Skipping commit {} (no Java source changes), no previous API stats to reuse", sha);
					}
					continue;
				}

				sw.reset().start();
				RepositoryWalkerUtils.makePristine(git);
				git.checkout()
					.setName(sha)
					.setForced(true)
					.call();
				long checkoutTime = sw.elapsed().toMillis();

				LOGGER.info("Commit {} on {}: {}", sha, date, msg);

				if (sources.stream().noneMatch(Files::exists)/* || poms.stream().noneMatch(Files::exists)*/) {
					LOGGER.info("Skipping.");
					continue;
				}

				long classpathTime = 0L;

				Path src = sources.stream().filter(Files::exists).findFirst().orElseThrow();
				sw.reset().start();
				API currentApi = RepositoryWalkerUtils.buildApi(src, classpath);
				long apiTime = sw.elapsed().toMillis();
				sw.reset().start();
				RepositoryWalkerUtils.ApiStats currentStats = RepositoryWalkerUtils.computeApiStats(currentApi);
				long statsTime = sw.elapsed().toMillis();

				if (oldApi != null) {
					sw.reset().start();
					RoseauReport diff = Roseau.diff(oldApi, currentApi);
					long diffTime = sw.elapsed().toMillis();

					List<BreakingChange> bcs = diff.getBreakingChanges();
					LOGGER.info("Found {} breaking changes", bcs.size());

					csvWriter.write(RepositoryWalkerUtils.csvLine(
						sha, date, msg, url, currentStats,
						checkoutTime, classpathTime, apiTime, diffTime, statsTime,
						bcs.size(), RepositoryWalkerUtils.breakingChangesToCsvCell(bcs)));
				}

				oldApi = currentApi;
				oldStats = currentStats;
			}
		}
	}
}
