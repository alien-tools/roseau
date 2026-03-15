package io.github.alien.roseau.git;

import io.github.alien.roseau.options.RoseauOptions;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GitWalkerTest {
	private static final RoseauOptions.Exclude NO_EXCLUSIONS =
		new RoseauOptions.Exclude(List.of(), List.of());

	private GitWalker walkerForRepo(Path remoteDir, Path wd, List<Path> sourceRoots) {
		Path cloneRoot = wd.resolve("clone");
		List<Path> roots = sourceRoots != null ? sourceRoots : List.of(cloneRoot.resolve("src/main/java"));
		return new GitWalker(new GitWalker.Config("test-lib", remoteDir.toUri().toString(),
			cloneRoot.resolve(".git"), roots, NO_EXCLUSIONS));
	}

	private GitWalker walkerForRepo(Path remoteDir, Path wd) {
		return walkerForRepo(remoteDir, wd, null);
	}

	private List<CommitAnalysis> collectAnalyses(GitWalker walker) throws Exception {
		List<CommitAnalysis> analyses = new ArrayList<>();
		walker.walk(analyses::add);
		return analyses;
	}

	// --- Commit ordering and count ---

	@Test
	void walk_emits_commits_oldest_to_newest(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
			GitWalkTestUtils.commit(remote, "c2",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void m() {} }"),
				List.of());
			GitWalkTestUtils.commit(remote, "c3",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void m() {} public void n() {} }"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(3);
		assertThat(analyses.get(0).commit().commitTime())
			.isBeforeOrEqualTo(analyses.get(1).commit().commitTime());
		assertThat(analyses.get(1).commit().commitTime())
			.isBeforeOrEqualTo(analyses.get(2).commit().commitTime());
	}

	// --- Skip logic ---

	@Test
	void walk_skips_non_java_commits_before_first_java_commit(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		RevCommit javaCommit;
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1-readme",
				Map.of("README.md", "hello"),
				List.of());
			GitWalkTestUtils.commit(remote, "c2-config",
				Map.of("config.yml", "key: value"),
				List.of());
			javaCommit = GitWalkTestUtils.commit(remote, "c3-java",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(1);
		assertThat(analyses.getFirst().commit().sha()).isEqualTo(javaCommit.getName());
	}

	@Test
	void walk_emits_non_java_commit_after_first_java_commit(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		RevCommit docsCommit;
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1-java",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
			docsCommit = GitWalkTestUtils.commit(remote, "c2-docs",
				Map.of("README.md", "docs"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(2);
		CommitAnalysis docsAnalysis = analyses.get(1);
		assertThat(docsAnalysis.commit().sha()).isEqualTo(docsCommit.getName());
		assertThat(docsAnalysis.apiChanged()).isFalse();
		assertThat(docsAnalysis.report()).isEmpty();
		assertThat(docsAnalysis.api()).isPresent();
		assertThat(docsAnalysis.checkoutTimeMs()).isZero();
		assertThat(docsAnalysis.apiTimeMs()).isZero();
		assertThat(docsAnalysis.diffTimeMs()).isZero();
	}

	// --- First commit behavior ---

	@Test
	void first_java_commit_has_api_but_no_report(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1-initial",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void m() {} }"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(1);
		CommitAnalysis first = analyses.getFirst();
		assertThat(first.api()).isPresent();
		assertThat(first.report()).isEmpty();
		assertThat(first.apiChanged()).isFalse();
		assertThat(first.errors()).isEmpty();
	}

	// --- API change detection ---

	@Test
	void breaking_change_sets_api_changed_and_produces_report(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void m() {} }"),
				List.of());
			GitWalkTestUtils.commit(remote, "c2-remove-method",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(2);
		CommitAnalysis breaking = analyses.get(1);
		assertThat(breaking.apiChanged()).isTrue();
		assertThat(breaking.report()).isPresent();
		assertThat(breaking.report().get().getAllBreakingChanges()).isNotEmpty();
		assertThat(breaking.errors()).isEmpty();
	}

	@Test
	void non_breaking_api_change_sets_api_changed_with_empty_breaking_changes(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
			GitWalkTestUtils.commit(remote, "c2-add-method",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void m() {} }"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(2);
		CommitAnalysis additive = analyses.get(1);
		assertThat(additive.apiChanged()).isTrue();
		assertThat(additive.report()).isPresent();
		assertThat(additive.report().get().getAllBreakingChanges()).isEmpty();
	}

	@Test
	void implementation_only_change_does_not_change_api(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1",
				Map.of("src/main/java/pkg/A.java",
					"package pkg; public class A { public int m() { return 1; } }"),
				List.of());
			GitWalkTestUtils.commit(remote, "c2-impl-change",
				Map.of("src/main/java/pkg/A.java",
					"package pkg; public class A { public int m() { return 2; } }"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(2);
		CommitAnalysis implChange = analyses.get(1);
		assertThat(implChange.apiChanged()).isFalse();
		assertThat(implChange.report()).isEmpty();
	}

	// --- CommitInfo fields ---

	@Test
	void commit_info_captures_java_diff_details(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1",
				Map.of(
					"src/main/java/pkg/A.java", "package pkg; public class A {}",
					"src/main/java/pkg/C.java", "package pkg; public class C {}"),
				List.of());
			GitWalkTestUtils.commit(remote, "c2",
				Map.of(
					"src/main/java/pkg/A.java", "package pkg; public class A { public void m() {} }",
					"src/main/java/pkg/B.java", "package pkg; public class B {}"),
				List.of("src/main/java/pkg/C.java"));
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));
		CommitInfo info = analyses.get(1).commit();

		assertThat(info.javaChanged()).isTrue();
		assertThat(info.updatedJavaFiles()).isNotEmpty();
		assertThat(info.createdJavaFiles()).isNotEmpty();
		assertThat(info.deletedJavaFiles()).isNotEmpty();
		assertThat(info.filesChanged()).isPositive();
		assertThat(info.locAdded()).isPositive();
	}

	@Test
	void commit_info_reflects_non_java_commit(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1-java",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
			GitWalkTestUtils.commit(remote, "c2-docs",
				Map.of("README.md", "docs"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));
		CommitInfo docsInfo = analyses.get(1).commit();

		assertThat(docsInfo.javaChanged()).isFalse();
		assertThat(docsInfo.updatedJavaFiles()).isEmpty();
		assertThat(docsInfo.createdJavaFiles()).isEmpty();
		assertThat(docsInfo.deletedJavaFiles()).isEmpty();
	}

	// --- Single commit ---

	@Test
	void single_commit_repository_produces_one_analysis(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "only-commit",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void m() {} }"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(1);
		CommitAnalysis only = analyses.getFirst();
		assertThat(only.api()).isPresent();
		assertThat(only.report()).isEmpty();
		assertThat(only.apiChanged()).isFalse();
		assertThat(only.commit().parentSha()).isEmpty();
	}

	// --- Java file addition and deletion ---

	@Test
	void deleting_a_type_is_a_breaking_change(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1",
				Map.of(
					"src/main/java/pkg/A.java", "package pkg; public class A {}",
					"src/main/java/pkg/B.java", "package pkg; public class B {}"),
				List.of());
			GitWalkTestUtils.commit(remote, "c2-delete",
				Map.of("src/main/java/pkg/B.java", "package pkg; public class B {}"),
				List.of("src/main/java/pkg/A.java"));
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(2);
		CommitAnalysis deletion = analyses.get(1);
		assertThat(deletion.apiChanged()).isTrue();
		assertThat(deletion.report()).isPresent();
		assertThat(deletion.report().get().getAllBreakingChanges()).isNotEmpty();
	}

	@Test
	void adding_a_new_type_is_not_a_breaking_change(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
			GitWalkTestUtils.commit(remote, "c2-add",
				Map.of(
					"src/main/java/pkg/A.java", "package pkg; public class A {}",
					"src/main/java/pkg/B.java", "package pkg; public class B {}"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(2);
		CommitAnalysis addition = analyses.get(1);
		assertThat(addition.apiChanged()).isTrue();
		assertThat(addition.report()).isPresent();
		assertThat(addition.report().get().getAllBreakingChanges()).isEmpty();
	}

	// --- API continuity across non-Java commits ---

	@Test
	void api_is_preserved_across_non_java_commits(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1-java",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void m() {} }"),
				List.of());
			GitWalkTestUtils.commit(remote, "c2-docs",
				Map.of("README.md", "docs"),
				List.of());
			GitWalkTestUtils.commit(remote, "c3-breaking",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(3);
		// The non-Java commit should carry the API from c1
		assertThat(analyses.get(1).api()).isPresent();
		// c3 should diff against c1's API (carried through c2), detecting the breaking change
		assertThat(analyses.get(2).apiChanged()).isTrue();
		assertThat(analyses.get(2).report()).isPresent();
		assertThat(analyses.get(2).report().get().getAllBreakingChanges()).isNotEmpty();
	}

	// --- Conventional breaking change indicator ---

	@Test
	void conventional_breaking_indicator_is_captured(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "feat: initial API",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void m() {} }"),
				List.of());
			GitWalkTestUtils.commit(remote, "feat!: remove legacy method",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
			GitWalkTestUtils.commit(remote, "fix: non-breaking fix",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void n() {} }"),
				List.of());
		}

		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd));

		assertThat(analyses).hasSize(3);
		assertThat(analyses.get(0).commit().conventionalCommitTag()).isEqualTo("feat");
		assertThat(analyses.get(0).commit().isConventionalBreakingChange()).isFalse();

		assertThat(analyses.get(1).commit().conventionalCommitTag()).isEqualTo("feat");
		assertThat(analyses.get(1).commit().isConventionalBreakingChange()).isTrue();

		assertThat(analyses.get(2).commit().conventionalCommitTag()).isEqualTo("fix");
		assertThat(analyses.get(2).commit().isConventionalBreakingChange()).isFalse();
	}

	// --- Multiple source roots ---

	@Test
	void walk_uses_first_existing_source_root(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1",
				Map.of("core/src/main/java/pkg/A.java", "package pkg; public class A { public void m() {} }"),
				List.of());
		}

		Path cloneRoot = wd.resolve("clone");
		List<Path> sourceRoots = List.of(
			cloneRoot.resolve("src/main/java"),       // does not exist
			cloneRoot.resolve("core/src/main/java")   // exists
		);
		List<CommitAnalysis> analyses = collectAnalyses(walkerForRepo(remoteDir, wd, sourceRoots));

		assertThat(analyses).hasSize(1);
		assertThat(analyses.getFirst().api()).isPresent();
	}
}
