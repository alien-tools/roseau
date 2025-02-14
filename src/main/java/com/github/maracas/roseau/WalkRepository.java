package com.github.maracas.roseau;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.extractors.sources.SpoonAPIExtractor;
import com.github.maracas.roseau.extractors.sources.SpoonUtils;
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
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WalkRepository {
	public static void main(String[] args) throws Exception {
		// Roseau
		// walk("https://github.com/alien-tools/roseau", Path.of("walk-roseau"), "main", "v0.0.2", "src/main/java");

		// Guava
		// walk("https://github.com/google/guava", Path.of("walk-guava"), "master", "v10.0", "guava/src/main/java");

		// commons-lang
		walk("https://github.com/apache/commons-lang", Path.of("walk-commons-lang"), "master",
			"LANG_1_0_B1", List.of("src/main/java", "src/java"));
	}

	public static void walk(String url, Path clone, String branch, String tagName, List<String> srcRoot) throws Exception {
		Git git;
		var repoDir = clone.toFile();

		// Clone the repository if it doesn't exist
		if (!repoDir.exists()) {
			System.out.printf("Cloning %s%n", url);
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

		var extractor = new SpoonAPIExtractor();
		RevCommit commit = null;
		API previousApi = null;
		var sw = Stopwatch.createUnstarted();
		while ((commit = walk.next()) != null) {
			Date commitDate = Date.from(commit.getAuthorIdent().getWhenAsInstant());

			System.out.printf("Checkout %s @ %s...", commit.getName(), commitDate);
			sw.reset().start();
			git.checkout().setName(commit.getName()).call();
			var checkoutTime = sw.elapsed().toMillis();
			System.out.printf(" done in %sms%n", checkoutTime);

			System.out.print("Parsing...");
			sw.reset().start();
			var model =
				Files.exists(clone.resolve(srcRoot.get(0)))
					? SpoonUtils.buildModel(clone.resolve(srcRoot.get(0)), Duration.ofMinutes(1))
					: SpoonUtils.buildModel(clone.resolve(srcRoot.get(1)), Duration.ofMinutes(1));
			var parsingTime = sw.elapsed().toMillis();
			System.out.printf(" done in %sms%n", parsingTime);

			System.out.print("Extracting API...");
			sw.reset().start();
			var api = extractor.extractAPI(model);
			var apiTime = sw.elapsed().toMillis();
			System.out.printf(" done in %sms%n", apiTime);

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

				var line = "%s|%s|%s|%d|%d|%d|%d|%d|%d|%d|%d|%s%n".formatted(commit.getName(), commitDate,
					commit.getShortMessage().replace("|", ""), numTypes, numMethods, numFields,
					checkoutTime, parsingTime, apiTime, diffTime, bcs.size(),
					bcs.stream().map(BreakingChange::toString).collect(Collectors.joining(",")));
				Files.write(Path.of("git.csv"), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
			}

			previousApi = api;
		}
	}
}
