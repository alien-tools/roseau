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
	void load_config_supports_global_and_repository_exclusions(@TempDir Path wd) throws Exception {
		Path yaml = wd.resolve("walk.yaml");
		Files.writeString(yaml, """
			defaults:
			  exclusions:
			    names:
			      - ".*\\\\.internal\\\\..*"
			    annotations:
			      - name: Internal
			        args: {}
			repositories:
			  - libraryId: lib
			    url: "https://example.org/repo.git"
			    gitDir: "%s/.git"
			    sourceRoots:
			      - "%s/src/main/java"
			    exclusions:
			      annotations:
			        - name: com.google.common.annotations.Beta
			          args: {}
			""".formatted(wd, wd, wd));

		List<GitWalker.Config> repositories = BatchGitWalker.loadConfig(yaml);

		assertThat(repositories).hasSize(1);
		GitWalker.Config repo = repositories.getFirst();
		assertThat(repo.libraryId()).isEqualTo("lib");
		assertThat(repo.exclusions().names()).containsExactly(".*\\.internal\\..*");
		assertThat(repo.exclusions().annotations()).extracting(a -> a.name())
			.containsExactly("Internal", "com.google.common.annotations.Beta");
	}

	@Test
	void prepare_repository_clones_when_missing(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "initial",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
		}

		Path cloneGitDir = wd.resolve("clone/.git");
		GitWalker.prepareRepository(remoteDir.toUri().toString(), cloneGitDir);

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
		GitWalker.prepareRepository(remoteUrl, cloneGitDir);

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

			GitWalker.prepareRepository(remoteUrl, cloneGitDir);

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
	void compute_commit_diff_and_root_mapping(@TempDir Path wd) throws Exception {
		Path repoDir = wd.resolve("repo");
		try (Git git = GitWalkTestUtils.initRepo(repoDir);
		     RevWalk rw = new RevWalk(git.getRepository())) {
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

			GitWalker.CommitDiff diff = GitWalker.buildCommitDiff(git.getRepository(), second, rw);

			assertThat(diff.javaChanged()).isTrue();
			assertThat(diff.pomChanged()).isTrue();
			assertThat(diff.updatedJavaFiles()).containsExactly(Path.of("src/main/java/pkg/A.java"));
			assertThat(diff.createdJavaFiles()).containsExactly(Path.of("src/main/java/pkg/B.java"));
			assertThat(diff.deletedJavaFiles()).containsExactly(Path.of("src/main/java/pkg/C.java"));

			ChangedFiles changed = GitWalker.changedFilesForSourceRoot(diff, Path.of("src/main/java"));
			assertThat(changed.updatedFiles()).containsExactly(Path.of("pkg/A.java"));
			assertThat(changed.createdFiles()).containsExactly(Path.of("pkg/B.java"));
			assertThat(changed.deletedFiles()).containsExactly(Path.of("pkg/C.java"));
		}
	}
}
