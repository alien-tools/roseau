package com.github.maracas.roseau;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.extractors.sources.SpoonAPIExtractor;
import com.google.common.base.Stopwatch;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.stream.Collectors;

public class WalkGuava {
	public static void main(String[] args) {
		String repoPath = "walk-guava";
//		String repoPath = "walk-roseau";
		String remoteUrl = "https://github.com/google/guava";
//		String remoteUrl = "https://github.com/alien-tools/roseau";

		try {
			File repoDir = new File(repoPath);
			Git git;

			// Clone the repository if it doesn't exist
			if (!repoDir.exists()) {
				System.out.println("Cloning repository...");
				git = Git.cloneRepository()
					.setURI(remoteUrl)
					.setDirectory(repoDir)
					.setBranch("master")
//					.setBranch("main")
					.call();
			} else {
				FileRepositoryBuilder builder = new FileRepositoryBuilder();
				Repository repository = builder.setGitDir(new File(repoPath + "/.git"))
					.build();
				git = new Git(repository);
			}

//			git.checkout().setName("main").call();
			git.checkout().setName("master").call();

			// Resolve the commit corresponding to the tag
			Repository repository = git.getRepository();
			RevWalk walk = new RevWalk(repository);

			// Get all commits from tagCommit to HEAD
			walk.sort(RevSort.TOPO, true);
			walk.sort(RevSort.COMMIT_TIME_DESC, true);
			walk.sort(RevSort.REVERSE, true);

			var tag = walk.parseCommit(repository.resolve("v10.0"));
//			var tag = walk.parseCommit(repository.resolve("v0.0.2"));
//			var head = walk.parseCommit(repository.resolve("main"));
			var head = walk.parseCommit(repository.resolve("master"));
			walk.markStart(head);
			walk.markUninteresting(tag);

			var extractor = new SpoonAPIExtractor();
			RevCommit commit;
			API previousApi = null;

			while ((commit = walk.next()) != null) {
				PersonIdent authorIdent = commit.getAuthorIdent();
				Date authorDate = authorIdent.getWhen();

				System.out.println("Checking commit " + authorDate + " (" + commit.getShortMessage() + ") [" + commit.getType() + "]");
				git.checkout().setName(commit.getName()).call();
				System.out.println("Done checking");

				var sw = Stopwatch.createStarted();
				var api = extractor.extractAPI(Path.of("walk-guava/guava/src"));
				var apiTime = sw.elapsed().toMillis();
//				var api = extractor.extractAPI(Path.of("walk-roseau/src/main/java"));
				if (previousApi != null) {
					sw.reset().start();
					var bcs = new APIDiff(previousApi, api).diff();
					var diffTime = sw.elapsed().toMillis();
					System.out.println("Found " + bcs.size() + " breaking changes:");

					var line = "%s|%s|%s|%d|%d|%d|%s%n".formatted(commit.getName(), authorDate, commit.getShortMessage(),
						apiTime, diffTime, bcs.size(), bcs.stream().map(BreakingChange::toString).collect(Collectors.joining(",")));
					Files.write(Path.of("git.csv"), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
				}
				previousApi = api;
			}
			System.out.println("Walk complete!");
		} catch (IOException | GitAPIException e) {
			e.printStackTrace();
		}
	}
}
