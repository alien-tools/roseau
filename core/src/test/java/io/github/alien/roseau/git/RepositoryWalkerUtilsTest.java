package io.github.alien.roseau.git;

import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryWalkerUtilsTest {
	@Test
	void prepare_repository_clones_when_missing(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "initial",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
		}

		Path cloneGitDir = wd.resolve("clone/.git");
		RepositoryWalkerUtils.prepareRepository(remoteDir.toUri().toString(), cloneGitDir);

		assertThat(Files.isDirectory(cloneGitDir)).isTrue();
		assertThat(Files.isDirectory(wd.resolve("clone/src/main/java"))).isTrue();
	}

	@Test
	void prepare_repository_fetches_resets_and_cleans_existing_clone(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "initial",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void m(){} }"),
				List.of());
		}

		Path cloneGitDir = wd.resolve("clone/.git");
		String remoteUrl = remoteDir.toUri().toString();
		RepositoryWalkerUtils.prepareRepository(remoteUrl, cloneGitDir);

		Path cloneRoot = wd.resolve("clone");
		try (Git local = Git.open(cloneRoot.toFile());
		     Git remote = Git.open(remoteDir.toFile())) {
			// Dirty and diverge local clone
			GitWalkTestUtils.commit(local, "local-only",
				Map.of("src/main/java/pkg/LocalOnly.java", "package pkg; public class LocalOnly {}"),
				List.of());
			Files.writeString(cloneRoot.resolve("tmp.txt"), "dirty");

			// Advance remote
			RevCommit remoteHead = GitWalkTestUtils.commit(remote, "remote-update",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void n(){} }"),
				List.of());

			RepositoryWalkerUtils.prepareRepository(remoteUrl, cloneGitDir);

			try (Git cleanedLocal = Git.open(cloneRoot.toFile())) {
				assertThat(GitWalkTestUtils.status(cleanedLocal).isClean()).isTrue();
				assertThat(Files.exists(cloneRoot.resolve("tmp.txt"))).isFalse();
				assertThat(Files.exists(cloneRoot.resolve("src/main/java/pkg/LocalOnly.java"))).isFalse();
				assertThat(Files.readString(cloneRoot.resolve("src/main/java/pkg/A.java"))).contains("n()");
				assertThat(cleanedLocal.log().setMaxCount(1).call().iterator().next().getName()).isEqualTo(remoteHead.getName());
			}
		}
	}

	@Test
	void prepare_repository_unshallows_existing_clone(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void a(){} }"),
				List.of());
			GitWalkTestUtils.commit(remote, "c2",
				Map.of("src/main/java/pkg/B.java", "package pkg; public class B { public void b(){} }"),
				List.of());
			GitWalkTestUtils.commit(remote, "c3",
				Map.of("src/main/java/pkg/C.java", "package pkg; public class C { public void c(){} }"),
				List.of());
		}

		Path cloneRoot = wd.resolve("shallow-clone");
		Path cloneGitDir = cloneRoot.resolve(".git");
		String remoteUrl = remoteDir.toUri().toString();
		try (Git ignored = Git.cloneRepository()
			.setURI(remoteUrl)
			.setDirectory(cloneRoot.toFile())
			.setDepth(1)
			.call()) {
		}

		try (Git shallowClone = Git.open(cloneRoot.toFile())) {
			assertThat(shallowClone.getRepository().getObjectDatabase().getShallowCommits()).isNotEmpty();
		}

		RepositoryWalkerUtils.prepareRepository(remoteUrl, cloneGitDir);

		try (Git completeClone = Git.open(cloneRoot.toFile());
		     RevWalk rw = new RevWalk(completeClone.getRepository())) {
			assertThat(completeClone.getRepository().getObjectDatabase().getShallowCommits()).isEmpty();
			assertThat(RepositoryWalkerUtils.firstParentChain(completeClone.getRepository(), rw)).hasSize(3);
		}
	}

	@Test
	void compute_commit_diff_and_root_mapping(@TempDir Path wd) throws Exception {
		Path repoDir = wd.resolve("repo");
		try (Git git = GitWalkTestUtils.initRepo(repoDir)) {
			GitWalkTestUtils.commit(git, "base",
				Map.of(
					"src/main/java/pkg/A.java", "package pkg; public class A {}",
					"src/main/java/pkg/C.java", "package pkg; public class C {}",
					"pom.xml", "<project/>"
				),
				List.of());
			RevCommit second = GitWalkTestUtils.commit(git, "changes",
				Map.of(
					"src/main/java/pkg/A.java", "package pkg; public class A { int x; }",
					"src/main/java/pkg/B.java", "package pkg; public class B {}",
					"pom.xml", "<project><version>2</version></project>"
				),
				List.of("src/main/java/pkg/C.java"));

			RepositoryWalkerUtils.CommitDiff diff = RepositoryWalkerUtils.computeCommitDiff(git.getRepository(), second);

			assertThat(diff.javaChanged()).isTrue();
			assertThat(diff.pomChanged()).isTrue();
			assertThat(diff.updatedJavaFiles()).containsExactly(Path.of("src/main/java/pkg/A.java"));
			assertThat(diff.createdJavaFiles()).containsExactly(Path.of("src/main/java/pkg/B.java"));
			assertThat(diff.deletedJavaFiles()).containsExactly(Path.of("src/main/java/pkg/C.java"));

			ChangedFiles changed = RepositoryWalkerUtils.changedFilesForSourceRoot(diff, Path.of("src/main/java"));
			assertThat(changed.updatedFiles()).containsExactly(Path.of("pkg/A.java"));
			assertThat(changed.createdFiles()).containsExactly(Path.of("pkg/B.java"));
			assertThat(changed.deletedFiles()).containsExactly(Path.of("pkg/C.java"));
		}
	}
}
