package com.github.maracas.roseau;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.extractors.TimestampChangedFilesProvider;
import com.github.maracas.roseau.extractors.jdt.IncrementalJdtAPIExtractor;
import com.github.maracas.roseau.extractors.jdt.JdtAPIExtractor;
import com.google.common.base.Stopwatch;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WalkRepository {
	private final static Path clonesPath = Path.of("clones");
	private final static Path resultsPath = Path.of("results");

	private final static File githubReposFile = new File("github_repos.json").getAbsoluteFile();

	private final static Stopwatch sw = Stopwatch.createUnstarted();

	private final static String headerCsv = "commit|date|message|createdFilesCount|deletedFilesCount|updatedFilesCount|typesCount|methodsCount|fieldsCount|deprecatedAnnotationsCount|betaAnnotationsCount|internalTypesCount|checkoutTime|apiTime|diffTime|breakingChangesCount|breakingChanges\n";

	public static void main(String[] args) throws Exception {
		if (!githubReposFile.exists()) {
			throw new IllegalStateException("No github_repos.json file found");
		}
		if (!resultsPath.toFile().exists()) {
			Files.createDirectories(resultsPath);
		}

		var walk = new WalkRepository();

		var githubRepos = new ObjectMapper().readValue(githubReposFile, new TypeReference<List<GithubRepo>>() {});
		githubRepos.forEach(repo -> {
			try {
				walk.walk(repo);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	void walk(GithubRepo repo) throws Exception {
		var name = repo.name();
		var url = repo.url();
		var branch = repo.branch();
		var tagName = repo.tag();
		var srcRoots = repo.sources();
		var clonePath = clonesPath.resolve(name);
		var resultsFilePath = resultsPath.resolve(name + ".csv");

		Files.writeString(resultsFilePath, headerCsv, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		var git = getGit(url, clonePath, branch);
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
		var incrementalExtractor = new IncrementalJdtAPIExtractor();
		var provider = new TimestampChangedFilesProvider(resolveSources(clonePath, srcRoots));

		RevCommit commit;
		API previousApi = null;
		while ((commit = walk.next()) != null) {
			try {
				Date commitDate = Date.from(commit.getAuthorIdent().getWhenAsInstant());
				System.out.printf("Checkout %s @ %s...", commit.getName(), commitDate);
				sw.reset().start();

				try {
					git.checkout().setName(commit.getName()).call();
				} catch (CheckoutConflictException e) {
					System.err.println("\nConflict when checking out commit " + commit.getName() + " - " + e.getMessage());
					System.err.println("Trying to clean the directory and checkout again");

					try {
						FileUtils.cleanDirectory(clonePath.toFile());
						FileUtils.deleteDirectory(clonePath.toFile());

						git = getGit(url, clonePath, branch);
						git.checkout().setName(commit.getName()).call();
					} catch (CheckoutConflictException e1) {
						System.err.println(e.getMessage());
						break;
					}
				}

				var checkoutTime = sw.elapsed().toMillis();
				System.out.printf(" done in %sms%n", checkoutTime);

				Path srcRoot = resolveSources(clonePath, srcRoots);
				if (!provider.getSources().equals(srcRoot))
					provider = new TimestampChangedFilesProvider(srcRoot);

				if (previousApi == null) {
					System.out.print("Extracting API...");
					sw.reset().start();

					previousApi = extractor.extractAPI(srcRoot);

					var apiTime = sw.elapsed().toMillis();
					System.out.printf(" done in %sms%n", apiTime);
				} else {
					var changedFiles = provider.getChangedFiles();
					System.out.printf("Partial update:%n\t%d created: %s%n\t%d removed: %s%n\t%d changed: %s%n",
							changedFiles.createdFiles().size(), changedFiles.createdFiles().stream()
									.map(p -> p.getFileName().toString()).collect(Collectors.joining(", ")),
							changedFiles.deletedFiles().size(), changedFiles.deletedFiles().stream()
									.map(p -> p.getFileName().toString()).collect(Collectors.joining(", ")),
							changedFiles.updatedFiles().size(), changedFiles.updatedFiles().stream()
									.map(p -> p.getFileName().toString()).collect(Collectors.joining(", ")));

					System.out.print("Extracting partial API...");
					sw.reset().start();

					var nextApi = incrementalExtractor.refreshAPI(srcRoot, changedFiles, previousApi);

					var apiTime = sw.elapsed().toMillis();
					System.out.printf(" done in %sms%n", apiTime);

					System.out.print("Diffing...");
					sw.reset().start();

					var bcs = new APIDiff(previousApi, nextApi).diff();

					var diffTime = sw.elapsed().toMillis();
					System.out.printf(" done in %sms%n", diffTime);
					System.out.printf("Found %d breaking changes%n", bcs.size());

					var numTypes = nextApi.getExportedTypes().count();
					var numMethods = nextApi.getExportedTypes()
							.mapToInt(type -> type.getDeclaredMethods().size())
							.sum();
					var numFields = nextApi.getExportedTypes()
							.mapToInt(type -> type.getDeclaredFields().size())
							.sum();
					var numDeprecatedAnnotations = getApiAnnotationsCount(nextApi, "java.lang.Deprecated");
					var numBetaAnnotations = getApiAnnotationsCount(nextApi, "com.google.common.annotations.Beta");
					var numInternalTypes = nextApi.getExportedTypes()
							.filter(t -> t.getQualifiedName().contains(".internal."))
							.count();

					var line = "%s|%s|%s|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%s%n".formatted(
							commit.getName(), commitDate,
							commit.getShortMessage().replace("|", ""),
							changedFiles.createdFiles().size(), changedFiles.deletedFiles().size(), changedFiles.updatedFiles().size(),
							numTypes, numMethods, numFields, numDeprecatedAnnotations, numBetaAnnotations, numInternalTypes,
							checkoutTime, apiTime, diffTime,
							bcs.size(), bcs.stream().map(BreakingChange::toString).collect(Collectors.joining(",")));

					Files.writeString(resultsFilePath, line, StandardOpenOption.APPEND);

					previousApi = nextApi;
				}

				provider.refresh(previousApi, Instant.now().toEpochMilli());
			} catch (Exception e) {
				e.printStackTrace();

				previousApi = null;
			}
		}
	}

	private static Git getGit(String url, Path clonePath, String branch) throws Exception {
		var repoDir = clonePath.toFile();

		Git git;
		if (repoDir.exists()) {
			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			Repository repository = builder.setGitDir(clonePath.resolve(".git").toFile()).build();

			git = new Git(repository);
		} else {
			System.out.printf("Cloning %s...%n", url);

			git = Git.cloneRepository()
					.setURI(url)
					.setDirectory(repoDir)
					.setBranch(branch)
					.call();
		}

		try {
			git.checkout().setName(branch).call();

			return git;
		} catch (CheckoutConflictException | UnsupportedOperationException e) {
			try {
				FileUtils.cleanDirectory(repoDir);
				FileUtils.deleteDirectory(repoDir);

				git = getGit(url, clonePath, branch);
				git.checkout().setName(branch).call();

				return git;
			} catch (CheckoutConflictException e1) {
				System.err.println(e.getMessage());

				throw e1;
			}
		}
	}

	private static Path resolveSources(Path clone, List<String> srcRoot) {
		return srcRoot.stream()
				.map(src -> clone.resolve(src).toAbsolutePath())
				.filter(src -> src.toFile().exists())
				.findFirst()
				.get();
	}

	private static long getApiAnnotationsCount(API api, String annotationQualifiedName) {
		return api.getExportedTypes()
				.mapToLong(type -> {
					var typeAnnotationCount = type.getAnnotations().stream()
							.filter(a -> a.actualAnnotation().getQualifiedName().equals(annotationQualifiedName))
							.count();
					var fieldsAnnotationCount = type.getDeclaredFields().stream()
							.filter(f -> f.getAnnotations().stream().anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(annotationQualifiedName)))
							.count();
					var methodsAnnotationCount = type.getDeclaredMethods().stream()
							.filter(m -> m.getAnnotations().stream().anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(annotationQualifiedName)))
							.count();

					return typeAnnotationCount + fieldsAnnotationCount + methodsAnnotationCount;
				})
				.sum();
	}

	private record GithubRepo(String name, String url, String branch, String tag, List<String> sources) {}
}
