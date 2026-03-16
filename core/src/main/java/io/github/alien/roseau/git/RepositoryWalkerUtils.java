package io.github.alien.roseau.git;

import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
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
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Git infrastructure utilities: cloning, fetching, commit chain traversal, diff computation.
 */
final class RepositoryWalkerUtils {
	private static final Logger LOGGER = LogManager.getLogger(RepositoryWalkerUtils.class);

	private RepositoryWalkerUtils() {
	}

	// --- Package-private diff record ---

	record CommitDiff(
		boolean javaChanged,
		boolean pomChanged,
		int filesChanged,
		int locAdded,
		int locDeleted,
		Set<Path> updatedJavaFiles,
		Set<Path> deletedJavaFiles,
		Set<Path> createdJavaFiles
	) {
	}

	// --- Repository preparation ---

	static Path prepareRepository(String url, Path gitDir) throws Exception {
		Path workTree = workTreeFromGitDir(gitDir);
		if (!Files.exists(gitDir)) {
			LOGGER.info("Local clone not found for {}, cloning into {}", url, workTree);
			cloneRepository(url, workTree);
			return workTree;
		}

		LOGGER.info("Preparing existing clone at {} (remote {})", gitDir, url);
		FileRepositoryBuilder builder = new FileRepositoryBuilder().setGitDir(gitDir.toFile()).readEnvironment();
		try (Repository repo = builder.build(); Git git = new Git(repo)) {
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
		LOGGER.info("Deleting existing clone at {} and cloning {} again", workTree, url);
		deleteRecursively(workTree);
		cloneRepository(url, workTree);
		return workTree;
	}

	// --- Commit chain ---

	static List<RevCommit> firstParentChain(Repository repo, RevWalk rw) throws IOException {
		ObjectId headId = repo.resolve("HEAD");
		if (headId == null) {
			throw new IllegalStateException("Cannot resolve HEAD");
		}

		List<RevCommit> chain = new ArrayList<>();
		RevCommit cur = rw.parseCommit(headId);
		while (true) {
			chain.add(cur);
			if (cur.getParentCount() == 0) break;
			cur = rw.parseCommit(cur.getParent(0));
		}
		Collections.reverse(chain);
		return chain;
	}

	// --- Commit diff ---

	static CommitDiff computeCommitDiff(Repository repo, RevCommit commit, RevWalk rw) {
		try (ObjectReader reader = repo.newObjectReader();
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
				oldTree.reset(reader, Constants.EMPTY_TREE_ID);
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

				for (Edit edit : df.toFileHeader(e).toEditList()) {
					locAdded += edit.getEndB() - edit.getBeginB();
					locDeleted += edit.getEndA() - edit.getBeginA();
				}
			}

			boolean javaChanged = !updated.isEmpty() || !deleted.isEmpty() || !created.isEmpty();
			return new CommitDiff(javaChanged, pomChanged, diffs.size(), locAdded, locDeleted,
				Set.copyOf(updated), Set.copyOf(deleted), Set.copyOf(created));
		} catch (IOException e) {
			LOGGER.warn("Failed to compute commit diff for {}", commit.getName(), e);
			return new CommitDiff(true, true, 0, 0, 0, Set.of(), Set.of(), Set.of());
		}
	}

	// --- Source root mapping ---

	static Optional<Path> sourceRootRelativeToWorkTree(Path workTree, Path sourceRoot) {
		Path absoluteWorkTree = workTree.toAbsolutePath().normalize();
		Path absoluteSourceRoot = sourceRoot.toAbsolutePath().normalize();
		if (!absoluteSourceRoot.startsWith(absoluteWorkTree)) {
			return Optional.empty();
		}
		return Optional.of(absoluteWorkTree.relativize(absoluteSourceRoot));
	}

	static ChangedFiles changedFilesForSourceRoot(CommitDiff diff, Path sourceRootRelative) {
		return new ChangedFiles(
			relativizeUnderRoot(diff.updatedJavaFiles(), sourceRootRelative),
			relativizeUnderRoot(diff.deletedJavaFiles(), sourceRootRelative),
			relativizeUnderRoot(diff.createdJavaFiles(), sourceRootRelative)
		);
	}

	// --- Tags and branch ---

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

	static String defaultBranchName(Repository repo) throws Exception {
		return Optional.ofNullable(resolveRemoteBranchName(repo)).orElse("");
	}

	// --- Commit metadata helpers ---

	static String parentCommit(RevCommit commit) {
		return commit.getParentCount() > 0 ? commit.getParent(0).getName() : "";
	}

	// --- Working tree ---

	static void makePristine(Git git) throws Exception {
		LOGGER.debug("Resetting repository to HEAD");
		git.reset().setMode(ResetCommand.ResetType.HARD).call();
		LOGGER.debug("Cleaning untracked files and directories");
		git.clean().setCleanDirectories(true).setIgnore(false).call();
	}

	// --- URL helpers ---

	static String commitUrl(String repositoryUrl, String commitSha) {
		String base = githubRepositoryBaseUrl(repositoryUrl);
		if (base.isEmpty() || commitSha == null || commitSha.isBlank()) {
			return "";
		}
		return base + "/commit/" + commitSha;
	}

	// --- Private helpers ---

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
		LOGGER.info("Cloning {} into {}", url, workTree);
		Git.cloneRepository().setURI(url).setDirectory(workTree.toFile()).call().close();
	}

	private static void fetchAndAlignRepository(Git git, Repository repo) throws Exception {
		LOGGER.info("Fetching updates from origin for {}", repo.getDirectory());
		var fetch = git.fetch().setRemote("origin");
		if (isShallowRepository(repo)) {
			LOGGER.info("Repository {} is shallow, unshallowing during fetch", repo.getDirectory());
			fetch.setUnshallow(true);
		}
		fetch.call();
		alignToRemoteDefaultBranch(git, repo);
	}

	private static void alignToRemoteDefaultBranch(Git git, Repository repo) throws Exception {
		String branchName = resolveRemoteBranchName(repo);
		if (branchName == null) {
			LOGGER.warn("Could not resolve default remote branch for {}", repo.getDirectory());
			return;
		}
		LOGGER.info("Aligning local repository to origin/{}", branchName);

		if (repo.findRef("refs/heads/" + branchName) == null) {
			LOGGER.debug("Creating local branch {} from origin/{}", branchName, branchName);
			git.checkout().setCreateBranch(true).setName(branchName).setStartPoint("origin/" + branchName).call();
		} else {
			LOGGER.debug("Checking out existing local branch {}", branchName);
			git.checkout().setName(branchName).setForced(true).call();
		}
		LOGGER.debug("Hard-resetting branch {} to origin/{}", branchName, branchName);
		git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/" + branchName).call();
	}

	private static String resolveRemoteBranchName(Repository repo) throws Exception {
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

	private static boolean isShallowRepository(Repository repo) throws IOException {
		return !repo.getObjectDatabase().getShallowCommits().isEmpty();
	}

	private static String githubRepositoryBaseUrl(String repositoryUrl) {
		if (repositoryUrl == null || repositoryUrl.isBlank()) {
			return "";
		}
		String trimmed = repositoryUrl.trim();
		if (trimmed.startsWith("git@github.com:")) {
			return toGithubBaseUrl(trimmed.substring("git@github.com:".length()));
		}
		try {
			URI uri = URI.create(trimmed);
			String host = uri.getHost();
			if (host == null || !host.equalsIgnoreCase("github.com")) {
				return "";
			}
			return toGithubBaseUrl(uri.getPath());
		} catch (IllegalArgumentException _) {
			return "";
		}
	}

	private static String toGithubBaseUrl(String repositoryPath) {
		if (repositoryPath == null || repositoryPath.isBlank()) {
			return "";
		}
		String normalized = repositoryPath.strip();
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		if (normalized.endsWith(".git")) {
			normalized = normalized.substring(0, normalized.length() - 4);
		}
		String[] segments = normalized.split("/");
		if (segments.length < 2 || segments[0].isBlank() || segments[1].isBlank()) {
			return "";
		}
		return "https://github.com/" + segments[0] + "/" + segments[1];
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
