package io.github.alien.roseau.git;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.extractors.TypesExtractor;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class RepositoryWalkerUtils {
	private static final Logger LOGGER = LogManager.getLogger(RepositoryWalkerUtils.class);
	static final String HEADER = "commit|date|message|commit_url|" +
		"typesCount|methodsCount|fieldsCount|deprecatedAnnotationsCount|betaAnnotationsCount|" +
		"checkoutTime|classpathTime|apiTime|diffTime|statsTime|" +
		"breakingChangesCount|breakingChanges\n";

	record Repository(
		String id,
		String url,
		Path gitDir,
		List<Path> sourceRoots,
		Path csv
	) {
	}

	record Flags(boolean javaChanged, boolean pomChanged) {
	}

	record ApiStats(
		int typesCount,
		int methodsCount,
		int fieldsCount,
		long deprecatedAnnotationsCount,
		long betaAnnotationsCount
	) {
	}

	record CommitDiff(
		boolean javaChanged,
		boolean pomChanged,
		Set<Path> updatedJavaFiles,
		Set<Path> deletedJavaFiles,
		Set<Path> createdJavaFiles
	) {
	}

	private static final ObjectMapper MAPPER = createMapper();

	private RepositoryWalkerUtils() {
	}

	static Path prepareRepository(String url, Path gitDir) throws Exception {
		Path workTree = workTreeFromGitDir(gitDir);
		if (!Files.exists(gitDir)) {
			cloneRepository(url, workTree);
			return workTree;
		}

		FileRepositoryBuilder builder = new FileRepositoryBuilder().setGitDir(gitDir.toFile()).readEnvironment();
		try (org.eclipse.jgit.lib.Repository repo = builder.build(); Git git = new Git(repo)) {
			fetchAndAlignRepository(git, repo);
			makePristine(git);
		} catch (Exception e) {
			if (!isMissingObjectFailure(e)) {
				throw e;
			}
			LOGGER.warn("Repository {} has missing objects, deleting local clone and cloning again", gitDir, e);
			recloneRepository(url, gitDir);
		}
		return workTree;
	}

	static boolean isMissingObjectFailure(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof MissingObjectException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	static Path recloneRepository(String url, Path gitDir) throws Exception {
		Path workTree = workTreeFromGitDir(gitDir);
		deleteRecursively(workTree);
		cloneRepository(url, workTree);
		return workTree;
	}

	private static ObjectMapper createMapper() {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());

		SimpleModule pathModule = new SimpleModule();
		pathModule.addDeserializer(Path.class,
			new com.fasterxml.jackson.databind.JsonDeserializer<>() {
				@Override
				public Path deserialize(com.fasterxml.jackson.core.JsonParser p,
				                        com.fasterxml.jackson.databind.DeserializationContext ctxt)
					throws IOException {
					return Path.of(p.getValueAsString());
				}
			});
		pathModule.addSerializer(Path.class,
			new com.fasterxml.jackson.databind.JsonSerializer<>() {
				@Override
				public void serialize(Path value,
				                      com.fasterxml.jackson.core.JsonGenerator gen,
				                      com.fasterxml.jackson.databind.SerializerProvider serializers)
					throws IOException {
					gen.writeString(value.toString());
				}
			});

		om.registerModule(pathModule);
		return om;
	}

	static List<Repository> loadConfig(Path yamlFile) throws IOException {
		return MAPPER.readValue(
			yamlFile.toFile(),
			new TypeReference<List<Repository>>() {
			}
		);
	}

	static List<RevCommit> firstParentChain(org.eclipse.jgit.lib.Repository repo, RevWalk rw) throws IOException {
		ObjectId headId = repo.resolve("HEAD");
		if (headId == null) {
			throw new IllegalStateException("Cannot resolve HEAD");
		}

		List<RevCommit> chain = new ArrayList<>();
		RevCommit cur = rw.parseCommit(headId);
		while (true) {
			chain.add(cur);
			if (cur.getParentCount() == 0) break;
			cur = rw.parseCommit(cur.getParent(0)); // first parent only
		}
		Collections.reverse(chain); // oldest -> newest
		return chain;
	}

	static CommitDiff computeCommitDiff(org.eclipse.jgit.lib.Repository repo, RevCommit commit) {
		try (RevWalk rw = new RevWalk(repo);
		     ObjectReader reader = repo.newObjectReader();
		     DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

			df.setRepository(repo);
			df.setDetectRenames(false);

			CanonicalTreeParser newTree = new CanonicalTreeParser();
			newTree.reset(reader, commit.getTree().getId());

			CanonicalTreeParser oldTree = new CanonicalTreeParser();
			if (commit.getParentCount() > 0) {
				RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
				oldTree.reset(reader, parent.getTree().getId());
			} else {
				ObjectId emptyTreeId = repo.resolve(Constants.EMPTY_TREE_ID.name());
				oldTree.reset(reader, emptyTreeId);
			}

			List<DiffEntry> diffs = df.scan(oldTree, newTree);

			Set<Path> updated = new HashSet<>();
			Set<Path> deleted = new HashSet<>();
			Set<Path> created = new HashSet<>();
			boolean pomChanged = false;

			for (DiffEntry e : diffs) {
				Path oldPath = pathOf(e.getOldPath());
				Path newPath = pathOf(e.getNewPath());

				if (!pomChanged && (isPomPath(oldPath) || isPomPath(newPath))) {
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
			}

			boolean javaChanged = !updated.isEmpty() || !deleted.isEmpty() || !created.isEmpty();
			return new CommitDiff(
				javaChanged,
				pomChanged,
				Set.copyOf(updated),
				Set.copyOf(deleted),
				Set.copyOf(created)
			);
		} catch (Exception _) {
			return new CommitDiff(true, true, Set.of(), Set.of(), Set.of());
		}
	}

	static Flags changedJavaOrPom(org.eclipse.jgit.lib.Repository repo, RevCommit commit) {
		CommitDiff diff = computeCommitDiff(repo, commit);
		return new Flags(diff.javaChanged(), diff.pomChanged());
	}

	static Optional<Path> sourceRootRelativeToWorkTree(Path workTree, Path sourceRoot) {
		Path absoluteWorkTree = workTree.toAbsolutePath().normalize();
		Path absoluteSourceRoot = sourceRoot.toAbsolutePath().normalize();
		if (!absoluteSourceRoot.startsWith(absoluteWorkTree)) {
			return Optional.empty();
		}
		return Optional.of(absoluteWorkTree.relativize(absoluteSourceRoot));
	}

	static ChangedFiles changedFilesForSourceRoot(CommitDiff diff, Path sourceRootRelative) {
		Set<Path> updated = relativizeUnderRoot(diff.updatedJavaFiles(), sourceRootRelative);
		Set<Path> deleted = relativizeUnderRoot(diff.deletedJavaFiles(), sourceRootRelative);
		Set<Path> created = relativizeUnderRoot(diff.createdJavaFiles(), sourceRootRelative);
		return new ChangedFiles(updated, deleted, created);
	}

	static API buildApi(Path sources, List<Path> classpath) {
		Library library = Library.builder()
			.location(sources)
			.classpath(classpath)
			.build();
		TypesExtractor extractor = new JdtTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
		LibraryTypes types = extractor.extractTypes(library);
		return types.toAPI();
	}

	static ApiStats computeApiStats(API api) {
		return new ApiStats(
			api.getExportedTypes().size(),
			api.getExportedTypes().stream()
				.mapToInt(type -> type.getDeclaredMethods().size())
				.sum(),
			api.getExportedTypes().stream()
				.mapToInt(type -> type.getDeclaredFields().size())
				.sum(),
			getApiAnnotationsCount(api, "java.lang.Deprecated"),
			getApiAnnotationsCount(api, "com.google.common.annotations.Beta")
		);
	}

	static String csvLine(
		String sha,
		Date date,
		String message,
		String url,
		ApiStats stats,
		long checkoutTime,
		long classpathTime,
		long apiTime,
		long diffTime,
		long statsTime,
		int breakingChangesCount,
		String breakingChanges
	) {
		return "%s|%s|%s|%s|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%s%n".formatted(
			sha, date, message.replace("|", " "), url + "/commit/" + sha,
			stats.typesCount(), stats.methodsCount(), stats.fieldsCount(),
			stats.deprecatedAnnotationsCount(), stats.betaAnnotationsCount(),
			checkoutTime, classpathTime, apiTime, diffTime, statsTime,
			breakingChangesCount, breakingChanges);
	}

	static String breakingChangesToCsvCell(List<BreakingChange> breakingChanges) {
		return breakingChanges.stream()
			.map(BreakingChange::toString)
			.collect(Collectors.joining(","));
	}

	static void makePristine(Git git) throws Exception {
		git.reset()
			.setMode(ResetCommand.ResetType.HARD)
			.call();

		git.clean()
			.setCleanDirectories(true)
			.setIgnore(false)
			.call();
	}

	private static Path workTreeFromGitDir(Path gitDir) {
		Path normalized = gitDir.toAbsolutePath().normalize();
		if (normalized.getFileName() != null && ".git".equals(normalized.getFileName().toString())) {
			return normalized.getParent();
		}
		return normalized;
	}

	private static void cloneRepository(String url, Path workTree) throws GitAPIException, IOException {
		if (workTree == null) {
			throw new IOException("Cannot resolve repository work tree");
		}
		Path parent = workTree.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Git.cloneRepository()
			.setURI(url)
			.setDirectory(workTree.toFile())
			.call()
			.close();
	}

	private static void fetchAndAlignRepository(Git git, org.eclipse.jgit.lib.Repository repo) throws Exception {
		var fetch = git.fetch().setRemote("origin");
		if (isShallowRepository(repo)) {
			fetch.setUnshallow(true);
		}
		fetch.call();
		alignToRemoteDefaultBranch(git, repo);
	}

	private static void alignToRemoteDefaultBranch(Git git, org.eclipse.jgit.lib.Repository repo) throws Exception {
		String branchName = resolveRemoteBranchName(repo);
		if (branchName == null) {
			return;
		}

		if (repo.findRef("refs/heads/" + branchName) == null) {
			git.checkout()
				.setCreateBranch(true)
				.setName(branchName)
				.setStartPoint("origin/" + branchName)
				.call();
		} else {
			git.checkout()
				.setName(branchName)
				.setForced(true)
				.call();
		}
		git.reset()
			.setMode(ResetCommand.ResetType.HARD)
			.setRef("origin/" + branchName)
			.call();
	}

	private static String resolveRemoteBranchName(org.eclipse.jgit.lib.Repository repo) throws Exception {
		final String prefix = "refs/remotes/origin/";

		Ref remoteHead = repo.exactRef("refs/remotes/origin/HEAD");
		if (remoteHead != null && remoteHead.getTarget() != null) {
			String remoteRefName = remoteHead.getTarget().getName();
			if (remoteRefName.startsWith(prefix)) {
				return remoteRefName.substring(prefix.length());
			}
		}

		for (String candidate : List.of("main", "master")) {
			if (repo.findRef(prefix + candidate) != null) {
				return candidate;
			}
		}

		String currentBranch = repo.getBranch();
		if (currentBranch != null && repo.findRef(prefix + currentBranch) != null) {
			return currentBranch;
		}

		return repo.getRefDatabase().getRefsByPrefix(prefix).stream()
			.map(ref -> ref.getName().substring(prefix.length()))
			.filter(name -> !name.equals("HEAD"))
			.findFirst()
			.orElse(null);
	}

	private static boolean isShallowRepository(org.eclipse.jgit.lib.Repository repo) throws IOException {
		return !repo.getObjectDatabase().getShallowCommits().isEmpty();
	}

	private static void deleteRecursively(Path root) throws IOException {
		if (root == null || !Files.exists(root)) {
			return;
		}
		try (var paths = Files.walk(root)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					throw new RuntimeException("Failed to delete " + path, e);
				}
			});
		} catch (RuntimeException e) {
			if (e.getCause() instanceof IOException io) {
				throw io;
			}
			throw e;
		}
	}

	private static long getApiAnnotationsCount(API api, String fqn) {
		return api.getExportedTypes().stream()
			.mapToLong(type -> {
				long typeAnnotationCount = type.getAnnotations().stream()
					.filter(a -> a.actualAnnotation().getQualifiedName().equals(fqn))
					.count();
				long fieldsAnnotationCount = type.getDeclaredFields().stream()
					.filter(f -> f.getAnnotations().stream().anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(fqn)))
					.count();
				long methodsAnnotationCount = type.getDeclaredMethods().stream()
					.filter(m -> m.getAnnotations().stream().anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(fqn)))
					.count();
				return typeAnnotationCount + fieldsAnnotationCount + methodsAnnotationCount;
			})
			.sum();
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
