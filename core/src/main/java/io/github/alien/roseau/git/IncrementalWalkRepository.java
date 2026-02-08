package io.github.alien.roseau.git;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
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
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Incremental commit walker: builds APIs incrementally from one commit to the next.
 *
 * TODO:
 *   - Account for library-specific exclusions
 *   - Always ignore *.internal.*?
 */
public class IncrementalWalkRepository {
	private static final Logger LOGGER = LogManager.getLogger(IncrementalWalkRepository.class);

	static void main() throws Exception {
		Path config = Path.of("walk.yaml");
		List<RepositoryWalkerUtils.Repository> repos = RepositoryWalkerUtils.loadConfig(config);
		repos.forEach(repo -> {
			try {
				walk(repo.url(), repo.gitDir(), repo.sourceRoots(), repo.csv());
			} catch (Exception e) {
				LOGGER.error("Incremental analysis of {} failed", repo.url(), e);
			}
		});
	}

	static void walk(String url, Path gitDir, List<Path> sourceRoots, Path csv) throws Exception {
		try {
			walkOnce(url, gitDir, sourceRoots, csv);
		} catch (Exception e) {
			if (!RepositoryWalkerUtils.isMissingObjectFailure(e)) {
				throw e;
			}
			LOGGER.warn("Repository {} is missing objects during checkout, re-cloning and retrying once", gitDir, e);
			RepositoryWalkerUtils.recloneRepository(url, gitDir);
			walkOnce(url, gitDir, sourceRoots, csv);
		}
	}

	private static void walkOnce(String url, Path gitDir, List<Path> sourceRoots, Path csv) throws Exception {
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
			LOGGER.info("Incrementally walking {} commits", chain.size());

			API oldApi = null;
			RepositoryWalkerUtils.ApiStats oldStats = null;
			Path oldSourceRoot = null;
			List<Path> classpath = List.of();

			JdtTypesExtractor jdtExtractor = new JdtTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
			IncrementalTypesExtractor incrementalExtractor = new IncrementalJdtTypesExtractor(jdtExtractor);

			for (RevCommit commit : chain) {
				String sha = commit.getName();
				Date date = Date.from(Instant.ofEpochSecond(commit.getCommitTime()));
				String msg = commit.getShortMessage();

				RepositoryWalkerUtils.CommitDiff commitDiff = RepositoryWalkerUtils.computeCommitDiff(repo, commit);
				if (!commitDiff.javaChanged()) {
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

				Optional<Path> srcOpt = sourceRoots.stream().filter(Files::exists).findFirst();
				if (srcOpt.isEmpty()) {
					LOGGER.info("Skipping commit {} (no configured source root exists)", sha);
					continue;
				}
				Path sourceRoot = srcOpt.get();
				LOGGER.info("Commit {} on {}: {} (source root {})", sha, date, msg, sourceRoot);

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
					currentApi = RepositoryWalkerUtils.buildApi(sourceRoot, classpath);
				} else {
					Optional<Path> sourceRootRelative = RepositoryWalkerUtils.sourceRootRelativeToWorkTree(
						repo.getWorkTree().toPath(), sourceRoot);
					if (sourceRootRelative.isEmpty()) {
						LOGGER.warn("Source root {} is outside repository root {}; falling back to full rebuild",
							sourceRoot, repo.getWorkTree());
						currentApi = RepositoryWalkerUtils.buildApi(sourceRoot, classpath);
					} else {
						ChangedFiles changedFiles = RepositoryWalkerUtils.changedFilesForSourceRoot(commitDiff, sourceRootRelative.get());
						if (changedFiles.hasNoChanges()) {
							currentApi = oldApi;
						} else {
							try {
								Library currentLibrary = Library.builder()
									.location(sourceRoot)
									.classpath(classpath)
									.build();
								LibraryTypes updatedTypes = incrementalExtractor.incrementalUpdate(
									oldApi.getLibraryTypes(), currentLibrary, changedFiles);
								currentApi = updatedTypes.toAPI();
							} catch (RuntimeException e) {
								LOGGER.warn("Incremental update failed for commit {}; falling back to full rebuild", sha, e);
								currentApi = RepositoryWalkerUtils.buildApi(sourceRoot, classpath);
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
					currentStats = RepositoryWalkerUtils.computeApiStats(currentApi);
					statsTime = sw.elapsed().toMillis();
				}

				if (oldApi != null) {
					long diffTime;
					List<BreakingChange> bcs;
					if (currentApi == oldApi) {
						diffTime = 0;
						bcs = List.of();
					} else {
						sw.reset().start();
						RoseauReport diff = Roseau.diff(oldApi, currentApi);
						diffTime = sw.elapsed().toMillis();
						bcs = diff.getBreakingChanges();
					}
					LOGGER.info("Found {} breaking changes", bcs.size());

					csvWriter.write(RepositoryWalkerUtils.csvLine(
						sha, date, msg, url, currentStats,
						checkoutTime, classpathTime, apiTime, diffTime, statsTime,
						bcs.size(), RepositoryWalkerUtils.breakingChangesToCsvCell(bcs)));
				}

				oldApi = currentApi;
				oldStats = currentStats;
				oldSourceRoot = sourceRoot;
			}
		}
	}
}
