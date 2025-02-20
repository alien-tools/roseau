package com.github.maracas.roseau;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.extractors.TimestampChangedFilesProvider;
import com.github.maracas.roseau.extractors.jdt.IncrementalJdtAPIExtractor;
import com.github.maracas.roseau.extractors.jdt.JdtAPIExtractor;
import com.google.common.base.Stopwatch;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WalkRepository {
	public static void main(String[] args) throws Exception {
		// Roseau
		// walk("https://github.com/alien-tools/roseau", Path.of("walk-roseau"), "main", "v0.0.2", "src/main/java");

		// Guava
		walk("https://github.com/google/guava", Path.of("walk-guava"), "master", "v10.0", List.of("guava/src"));

		// commons-lang
		// walk("https://github.com/apache/commons-lang", Path.of("walk-commons-lang"), "master",
		// 	"LANG_1_0_B1", List.of("src/main/java", "src/java"));
	}

	public static void walk(String url, Path clone, String branch, String tagName, List<String> srcRoot) throws Exception {
		Git git;
		var repoDir = clone.toFile();

		// Clone the repository if it doesn't exist
		if (!repoDir.exists()) {
			System.out.printf("Cloning %s...%n", url);
			git = Git.cloneRepository()
				.setURI(url)
				.setDirectory(repoDir)
				.setBranch(branch)
				.call();
		} else {
			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			Repository repository = builder.setGitDir(clone.resolve(".git").toFile())
				.build();
			git = new Git(repository);
		}

		// Rewind to HEAD
		git.checkout().setName(branch).call();

		var repository = git.getRepository();
		var walk = new RevWalk(repository);

		// Setup traversal
		walk.sort(RevSort.TOPO, true);
		walk.sort(RevSort.COMMIT_TIME_DESC, true);
		walk.sort(RevSort.REVERSE, true);

		var tag = walk.parseCommit(repository.resolve(tagName));
		var head = walk.parseCommit(repository.resolve(branch));
		walk.markStart(head);
		walk.markUninteresting(tag);

		var extractor = new JdtAPIExtractor();
		RevCommit commit = null;
		API previousApi = null;
		var sw = Stopwatch.createUnstarted();
		var provider = new TimestampChangedFilesProvider(clone.resolve(srcRoot.getFirst()).toAbsolutePath());
		var incrementalExtractor = new IncrementalJdtAPIExtractor();
		while ((commit = walk.next()) != null) {
			Date commitDate = Date.from(commit.getAuthorIdent().getWhenAsInstant());

			System.out.printf("Checkout %s @ %s...", commit.getName(), commitDate);
			sw.reset().start();
			git.checkout().setName(commit.getName()).call();
			var checkoutTime = sw.elapsed().toMillis();
			System.out.printf(" done in %sms%n", checkoutTime);

			API api = null;
			long apiTime = 0;
			var changedFiles = provider.getChangedFiles();
			if (previousApi == null) {
				System.out.print("Extracting API...");
				sw.reset().start();
				api = Files.exists(clone.resolve(srcRoot.get(0)))
					? extractor.extractAPI(clone.resolve(srcRoot.get(0)))
					: extractor.extractAPI(clone.resolve(srcRoot.get(1)));
				apiTime = sw.elapsed().toMillis();
				System.out.printf(" done in %sms%n", apiTime);
			} else {
				System.out.printf("Partial update:%n\t%d created: %s%n\t%d removed: %s%n\t%d changed: %s%n",
					changedFiles.createdFiles().size(), changedFiles.createdFiles().stream()
						.map(p -> p.getFileName().toString()).collect(Collectors.joining(", ")),
					changedFiles.deletedFiles().size(), changedFiles.deletedFiles().stream()
						.map(p -> p.getFileName().toString()).collect(Collectors.joining(", ")),
					changedFiles.updatedFiles().size(), changedFiles.updatedFiles().stream()
						.map(p -> p.getFileName().toString()).collect(Collectors.joining(", ")));
				System.out.print("Extracting partial API...");
				sw.reset().start();
				api = Files.exists(clone.resolve(srcRoot.get(0)))
					? incrementalExtractor.refreshAPI(clone.resolve(srcRoot.get(0)), changedFiles, previousApi)
					: incrementalExtractor.refreshAPI(clone.resolve(srcRoot.get(1)), changedFiles, previousApi);
				apiTime = sw.elapsed().toMillis();
				System.out.printf(" done in %sms%n", apiTime);
			}

			if (previousApi != null) {
				System.out.print("Diffing...");
				sw.reset().start();
				var bcs = new APIDiff(previousApi, api).diff();
				var diffTime = sw.elapsed().toMillis();
				System.out.printf(" done in %sms%n", diffTime);
				System.out.printf("Found %d breaking changes%n", bcs.size());

				long numTypes = api.getExportedTypes().count();
				int numMethods = api.getExportedTypes()
					.mapToInt(type -> type.getDeclaredMethods().size())
					.sum();
				int numFields = api.getExportedTypes()
					.mapToInt(type -> type.getDeclaredFields().size())
					.sum();

				var line = "%s|%s|%s|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%s%n".formatted(commit.getName(), commitDate,
					commit.getShortMessage().replace("|", ""),
					changedFiles.createdFiles().size(), changedFiles.deletedFiles().size(), changedFiles.updatedFiles().size(),
					numTypes, numMethods, numFields,
					checkoutTime, apiTime, diffTime, bcs.size(),
					bcs.stream().map(BreakingChange::toString).collect(Collectors.joining(",")));
				Files.write(Path.of("git.csv"), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
			}

			previousApi = api;
			provider.refresh(previousApi, Instant.now().toEpochMilli());
		}
	}
}

