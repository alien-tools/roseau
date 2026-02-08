package io.github.alien.roseau.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WalkersIT {
	private static final io.github.alien.roseau.RoseauOptions.Exclude NO_EXCLUSIONS =
		new io.github.alien.roseau.RoseauOptions.Exclude(List.of(), List.of());

	@Test
	void incremental_and_regular_walkers_match_on_simple_history(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		RevCommit c2;
		RevCommit c3;
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1-initial",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void m(){} }"),
				List.of());
			c2 = GitWalkTestUtils.commit(remote, "c2-docs",
				Map.of("README.md", "docs"),
				List.of());
			c3 = GitWalkTestUtils.commit(remote, "c3-breaking",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
		}

		Path regularOutputDir = wd.resolve("regular-out");
		Path incrementalOutputDir = wd.resolve("incremental-out");
		Path regularCommits = regularOutputDir.resolve("regular-commits.csv");
		Path regularBcs = regularOutputDir.resolve("regular-bcs.csv");
		Path incrementalCommits = incrementalOutputDir.resolve("incremental-commits.csv");
		Path incrementalBcs = incrementalOutputDir.resolve("incremental-bcs.csv");

		Path regularRoot = wd.resolve("regular-clone");
		Path incrementalRoot = wd.resolve("incremental-clone");
		String url = remoteDir.toUri().toString();
		WalkRepository.walk("regular", url, regularRoot.resolve(".git"), List.of(regularRoot.resolve("src/main/java")),
			List.of(), regularOutputDir, NO_EXCLUSIONS);
		IncrementalWalkRepository.walk("incremental", url, incrementalRoot.resolve(".git"),
			List.of(incrementalRoot.resolve("src/main/java")), incrementalOutputDir, NO_EXCLUSIONS);

		Map<String, GitWalkTestUtils.CommitCsvRow> regular = GitWalkTestUtils.readCommitCsvRows(regularCommits);
		Map<String, GitWalkTestUtils.CommitCsvRow> incremental = GitWalkTestUtils.readCommitCsvRows(incrementalCommits);
		Map<String, Integer> regularBcsCount = GitWalkTestUtils.readBreakingChangesCountByCommit(regularBcs);
		Map<String, Integer> incrementalBcsCount = GitWalkTestUtils.readBreakingChangesCountByCommit(incrementalBcs);

		assertThat(incremental.keySet()).isEqualTo(regular.keySet());
		assertThat(regular).containsKeys(c2.getName(), c3.getName());
		assertThat(incremental).containsKeys(c2.getName(), c3.getName());

		for (String sha : regular.keySet()) {
			var r = regular.get(sha);
			var i = incremental.get(sha);
			assertThat(i.exportedTypesCount()).isEqualTo(r.exportedTypesCount());
			assertThat(i.exportedMethodsCount()).isEqualTo(r.exportedMethodsCount());
			assertThat(i.exportedFieldsCount()).isEqualTo(r.exportedFieldsCount());
			assertThat(i.breakingChangesCount()).isEqualTo(r.breakingChangesCount());
		}
		assertThat(incrementalBcsCount).isEqualTo(regularBcsCount);

		assertThat(regular.get(c2.getName()).breakingChangesCount()).isZero();
		assertThat(regular.get(c2.getName()).apiTimeMs()).isZero();
		assertThat(regular.get(c3.getName()).breakingChangesCount()).isGreaterThan(0);
		assertThat(regularBcsCount.getOrDefault(c3.getName(), 0)).isGreaterThan(0);
	}

	@Test
	void incremental_and_regular_walkers_match_when_source_root_changes(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote");
		RevCommit c2;
		RevCommit c3;
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1-old-root",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void m(){} }"),
				List.of());
			c2 = GitWalkTestUtils.commit(remote, "c2-move-root",
				Map.of("core/src/main/java/pkg/A.java", "package pkg; public class A { public void m(){} }"),
				List.of("src/main/java/pkg/A.java"));
			c3 = GitWalkTestUtils.commit(remote, "c3-breaking",
				Map.of("core/src/main/java/pkg/A.java", "package pkg; public class A {}"),
				List.of());
		}

		Path regularOutputDir = wd.resolve("regular-root-switch-out");
		Path incrementalOutputDir = wd.resolve("incremental-root-switch-out");
		Path regularCommits = regularOutputDir.resolve("regular-root-switch-commits.csv");
		Path incrementalCommits = incrementalOutputDir.resolve("incremental-root-switch-commits.csv");
		Path regularBcs = regularOutputDir.resolve("regular-root-switch-bcs.csv");
		Path incrementalBcs = incrementalOutputDir.resolve("incremental-root-switch-bcs.csv");

		Path regularRoot = wd.resolve("regular-clone");
		Path incrementalRoot = wd.resolve("incremental-clone");
		List<Path> sourceRootsRegular = List.of(
			regularRoot.resolve("src/main/java"),
			regularRoot.resolve("core/src/main/java")
		);
		List<Path> sourceRootsIncremental = List.of(
			incrementalRoot.resolve("src/main/java"),
			incrementalRoot.resolve("core/src/main/java")
		);

		String url = remoteDir.toUri().toString();
		WalkRepository.walk("regular-root-switch", url, regularRoot.resolve(".git"), sourceRootsRegular, List.of(),
			regularOutputDir, NO_EXCLUSIONS);
		IncrementalWalkRepository.walk("incremental-root-switch", url, incrementalRoot.resolve(".git"),
			sourceRootsIncremental, incrementalOutputDir, NO_EXCLUSIONS);

		Map<String, GitWalkTestUtils.CommitCsvRow> regular = GitWalkTestUtils.readCommitCsvRows(regularCommits);
		Map<String, GitWalkTestUtils.CommitCsvRow> incremental = GitWalkTestUtils.readCommitCsvRows(incrementalCommits);
		Map<String, Integer> regularBcsCount = GitWalkTestUtils.readBreakingChangesCountByCommit(regularBcs);
		Map<String, Integer> incrementalBcsCount = GitWalkTestUtils.readBreakingChangesCountByCommit(incrementalBcs);

		assertThat(regular).containsKeys(c2.getName(), c3.getName());
		assertThat(incremental).containsKeys(c2.getName(), c3.getName());

		assertThat(incremental.keySet()).isEqualTo(regular.keySet());
		for (String sha : regular.keySet()) {
			var r = regular.get(sha);
			var i = incremental.get(sha);
			assertThat(i.exportedTypesCount()).isEqualTo(r.exportedTypesCount());
			assertThat(i.exportedMethodsCount()).isEqualTo(r.exportedMethodsCount());
			assertThat(i.exportedFieldsCount()).isEqualTo(r.exportedFieldsCount());
			assertThat(i.breakingChangesCount()).isEqualTo(r.breakingChangesCount());
		}
		assertThat(incrementalBcsCount).isEqualTo(regularBcsCount);
	}

	@Test
	void walkers_emit_all_tags_and_versions_for_each_commit(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote-tags");
		RevCommit c1;
		RevCommit c2;
		RevCommit c3;
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			c1 = GitWalkTestUtils.commit(remote, "c1-initial",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void a(){} }"),
				List.of());
			remote.tag().setName("baseline").setObjectId(c1).call();

			c2 = GitWalkTestUtils.commit(remote, "c2-change",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void b(){} }"),
				List.of());
			remote.tag().setName("v1.0.0").setObjectId(c2).call();
			remote.tag().setName("1.0.0").setObjectId(c2).call();
			remote.tag().setName("latest").setObjectId(c2).call();

			c3 = GitWalkTestUtils.commit(remote, "c3-change",
				Map.of("src/main/java/pkg/A.java", "package pkg; public class A { public void c(){} }"),
				List.of());
			remote.tag().setName("release").setObjectId(c3).call();
		}

		Path regularOutputDir = wd.resolve("regular-tags-out");
		Path incrementalOutputDir = wd.resolve("incremental-tags-out");
		Path regularCommits = regularOutputDir.resolve("regular-tags-commits.csv");
		Path incrementalCommits = incrementalOutputDir.resolve("incremental-tags-commits.csv");
		Path regularRoot = wd.resolve("regular-tags-clone");
		Path incrementalRoot = wd.resolve("incremental-tags-clone");
		String url = remoteDir.toUri().toString();

		WalkRepository.walk("regular-tags", url, regularRoot.resolve(".git"), List.of(regularRoot.resolve("src/main/java")),
			List.of(), regularOutputDir, NO_EXCLUSIONS);
		IncrementalWalkRepository.walk("incremental-tags", url, incrementalRoot.resolve(".git"),
			List.of(incrementalRoot.resolve("src/main/java")), incrementalOutputDir, NO_EXCLUSIONS);

		Map<String, Map<String, String>> regularRows = GitWalkTestUtils.readCsvRows(regularCommits).stream()
			.collect(java.util.stream.Collectors.toMap(r -> r.get("commit_sha"), r -> r));
		Map<String, Map<String, String>> incrementalRows = GitWalkTestUtils.readCsvRows(incrementalCommits).stream()
			.collect(java.util.stream.Collectors.toMap(r -> r.get("commit_sha"), r -> r));

		assertThat(regularRows).containsKeys(c1.getName(), c2.getName(), c3.getName());
		assertThat(incrementalRows).containsKeys(c1.getName(), c2.getName(), c3.getName());

		assertThat(regularRows.get(c2.getName()).get("tag")).isEqualTo("1.0.0;latest;v1.0.0");
		assertThat(regularRows.get(c2.getName()).get("version")).isEqualTo("1.0.0;latest;v1.0.0");
		assertThat(regularRows.get(c3.getName()).get("tag")).isEqualTo("release");
		assertThat(regularRows.get(c3.getName()).get("version")).isEqualTo("release");

		assertThat(incrementalRows.get(c2.getName()).get("tag")).isEqualTo("1.0.0;latest;v1.0.0");
		assertThat(incrementalRows.get(c2.getName()).get("version")).isEqualTo("1.0.0;latest;v1.0.0");
		assertThat(incrementalRows.get(c3.getName()).get("tag")).isEqualTo("release");
		assertThat(incrementalRows.get(c3.getName()).get("version")).isEqualTo("release");
	}

	@Test
	void emits_excluded_bcs_when_excluded_by_name(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote-name");
		RevCommit breaking;
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1-initial",
				Map.of("src/main/java/pkg/internal/A.java", "package pkg.internal; public class A { public void m(){} }"),
				List.of());
			breaking = GitWalkTestUtils.commit(remote, "c2-breaking",
				Map.of("src/main/java/pkg/internal/A.java", "package pkg.internal; public class A {}"),
				List.of());
		}

		Path outputDir = wd.resolve("name-out");
		Path bcsCsv = outputDir.resolve("name-case-bcs.csv");
		Path cloneRoot = wd.resolve("name-clone");
		String url = remoteDir.toUri().toString();
		var exclusions = new io.github.alien.roseau.RoseauOptions.Exclude(List.of(".*\\.internal\\..*"), List.of());
		WalkRepository.walk("name-case", url, cloneRoot.resolve(".git"), List.of(cloneRoot.resolve("src/main/java")),
			List.of(), outputDir, exclusions);

		List<Map<String, String>> rows = GitWalkTestUtils.readCsvRows(bcsCsv);
		assertThat(rows).isNotEmpty();
		assertThat(rows.stream().anyMatch(r ->
			r.get("commit").equals(breaking.getName()) &&
				Boolean.parseBoolean(r.get("is_excluded_symbol")))).isTrue();
	}

	@Test
	void emits_excluded_bcs_when_excluded_by_annotation_fqn(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote-ann-fqn");
		RevCommit breaking;
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1-initial",
				Map.of(
					"src/main/java/com/google/common/annotations/Beta.java",
					"package com.google.common.annotations; public @interface Beta {}",
					"src/main/java/pkg/A.java",
					"package pkg; import com.google.common.annotations.Beta; public class A { @Beta public void m(){} }"
				),
				List.of());
			breaking = GitWalkTestUtils.commit(remote, "c2-breaking",
				Map.of(
					"src/main/java/com/google/common/annotations/Beta.java",
					"package com.google.common.annotations; public @interface Beta {}",
					"src/main/java/pkg/A.java",
					"package pkg; import com.google.common.annotations.Beta; public class A {}"
				),
				List.of());
		}

		Path outputDir = wd.resolve("ann-fqn-out");
		Path bcsCsv = outputDir.resolve("ann-fqn-case-bcs.csv");
		Path cloneRoot = wd.resolve("ann-fqn-clone");
		String url = remoteDir.toUri().toString();
		var exclusions = new io.github.alien.roseau.RoseauOptions.Exclude(
			List.of(),
			List.of(new io.github.alien.roseau.RoseauOptions.AnnotationExclusion(
				"com.google.common.annotations.Beta", Map.of()))
		);
		WalkRepository.walk("ann-fqn-case", url, cloneRoot.resolve(".git"), List.of(cloneRoot.resolve("src/main/java")),
			List.of(), outputDir, exclusions);

		List<Map<String, String>> rows = GitWalkTestUtils.readCsvRows(bcsCsv);
		assertThat(rows).isNotEmpty();
		assertThat(rows.stream().anyMatch(r ->
			r.get("commit").equals(breaking.getName()) &&
				Boolean.parseBoolean(r.get("is_excluded_symbol")))).isTrue();
	}

	@Test
	void emits_excluded_bcs_when_excluded_by_annotation_simple_name(@TempDir Path wd) throws Exception {
		Path remoteDir = wd.resolve("remote-ann-simple");
		RevCommit breaking;
		try (Git remote = GitWalkTestUtils.initRepo(remoteDir)) {
			GitWalkTestUtils.commit(remote, "c1-initial",
				Map.of(
					"src/main/java/pkg/Internal.java",
					"package pkg; public @interface Internal {}",
					"src/main/java/pkg/A.java",
					"package pkg; public class A { @Internal public void m(){} }"
				),
				List.of());
			breaking = GitWalkTestUtils.commit(remote, "c2-breaking",
				Map.of(
					"src/main/java/pkg/Internal.java",
					"package pkg; public @interface Internal {}",
					"src/main/java/pkg/A.java",
					"package pkg; public class A {}"
				),
				List.of());
		}

		Path outputDir = wd.resolve("ann-simple-out");
		Path bcsCsv = outputDir.resolve("ann-simple-case-bcs.csv");
		Path cloneRoot = wd.resolve("ann-simple-clone");
		String url = remoteDir.toUri().toString();
		var exclusions = new io.github.alien.roseau.RoseauOptions.Exclude(
			List.of(),
			List.of(new io.github.alien.roseau.RoseauOptions.AnnotationExclusion("Internal", Map.of()))
		);
		WalkRepository.walk("ann-simple-case", url, cloneRoot.resolve(".git"), List.of(cloneRoot.resolve("src/main/java")),
			List.of(), outputDir, exclusions);

		List<Map<String, String>> rows = GitWalkTestUtils.readCsvRows(bcsCsv);
		assertThat(rows).isNotEmpty();
		assertThat(rows.stream().anyMatch(r ->
			r.get("commit").equals(breaking.getName()) &&
				Boolean.parseBoolean(r.get("is_excluded_symbol")))).isTrue();
	}
}
