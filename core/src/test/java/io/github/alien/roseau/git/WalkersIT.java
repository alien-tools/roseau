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
	private static final io.github.alien.roseau.options.RoseauOptions.Exclude NO_EXCLUSIONS =
		new io.github.alien.roseau.options.RoseauOptions.Exclude(List.of(), List.of());

	@Test
	void walker_produces_correct_output_on_simple_history(@TempDir Path wd) throws Exception {
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

		Path outputDir = wd.resolve("out");
		Path commitsCsv = outputDir.resolve("lib-commits.csv");
		Path bcsCsv = outputDir.resolve("lib-bcs.csv");
		Path cloneRoot = wd.resolve("clone");
		String url = remoteDir.toUri().toString();

		IncrementalWalkRepository.walk("lib", url, cloneRoot.resolve(".git"),
			List.of(cloneRoot.resolve("src/main/java")), outputDir, NO_EXCLUSIONS);

		Map<String, GitWalkTestUtils.CommitCsvRow> commits = GitWalkTestUtils.readCommitCsvRows(commitsCsv);
		Map<String, Integer> bcsCount = GitWalkTestUtils.readBreakingChangesCountByCommit(bcsCsv);

		assertThat(commits).containsKeys(c2.getName(), c3.getName());

		assertThat(commits.get(c2.getName()).updatedJavaFilesCount()).isZero();
		assertThat(commits.get(c2.getName()).deletedJavaFilesCount()).isZero();
		assertThat(commits.get(c2.getName()).createdJavaFilesCount()).isZero();
		assertThat(commits.get(c2.getName()).apiChanged()).isFalse();
		assertThat(commits.get(c2.getName()).breakingChangesCount()).isZero();
		assertThat(commits.get(c2.getName()).apiTimeMs()).isZero();

		assertThat(commits.get(c3.getName()).updatedJavaFilesCount()).isEqualTo(1);
		assertThat(commits.get(c3.getName()).deletedJavaFilesCount()).isZero();
		assertThat(commits.get(c3.getName()).createdJavaFilesCount()).isZero();
		assertThat(commits.get(c3.getName()).apiChanged()).isTrue();
		assertThat(commits.get(c3.getName()).breakingChangesCount()).isGreaterThan(0);
		assertThat(bcsCount.getOrDefault(c3.getName(), 0)).isGreaterThan(0);
	}

	@Test
	void walker_handles_source_root_changes(@TempDir Path wd) throws Exception {
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

		Path outputDir = wd.resolve("out");
		Path commitsCsv = outputDir.resolve("lib-commits.csv");
		Path bcsCsv = outputDir.resolve("lib-bcs.csv");
		Path cloneRoot = wd.resolve("clone");
		List<Path> sourceRoots = List.of(
			cloneRoot.resolve("src/main/java"),
			cloneRoot.resolve("core/src/main/java")
		);

		String url = remoteDir.toUri().toString();
		IncrementalWalkRepository.walk("lib", url, cloneRoot.resolve(".git"),
			sourceRoots, outputDir, NO_EXCLUSIONS);

		Map<String, GitWalkTestUtils.CommitCsvRow> commits = GitWalkTestUtils.readCommitCsvRows(commitsCsv);
		Map<String, Integer> bcsCount = GitWalkTestUtils.readBreakingChangesCountByCommit(bcsCsv);

		assertThat(commits).containsKeys(c2.getName(), c3.getName());

		// c2 moves the source root; the file is recreated at the new path
		assertThat(commits.get(c2.getName()).exportedTypesCount()).isEqualTo(1);

		assertThat(commits.get(c3.getName()).apiChanged()).isTrue();
		assertThat(commits.get(c3.getName()).breakingChangesCount()).isGreaterThan(0);
		assertThat(bcsCount.getOrDefault(c3.getName(), 0)).isGreaterThan(0);
	}

	@Test
	void walker_emits_all_tags_and_versions_for_each_commit(@TempDir Path wd) throws Exception {
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

		Path outputDir = wd.resolve("out");
		Path commitsCsv = outputDir.resolve("lib-tags-commits.csv");
		Path cloneRoot = wd.resolve("clone");
		String url = remoteDir.toUri().toString();

		IncrementalWalkRepository.walk("lib-tags", url, cloneRoot.resolve(".git"),
			List.of(cloneRoot.resolve("src/main/java")), outputDir, NO_EXCLUSIONS);

		Map<String, Map<String, String>> rows = GitWalkTestUtils.readCsvRows(commitsCsv).stream()
			.collect(java.util.stream.Collectors.toMap(r -> r.get("commit_sha"), r -> r));

		assertThat(rows).containsKeys(c1.getName(), c2.getName(), c3.getName());

		assertThat(rows.get(c2.getName()).get("tag")).isEqualTo("1.0.0;latest;v1.0.0");
		assertThat(rows.get(c2.getName()).get("version")).isEqualTo("1.0.0;latest;v1.0.0");
		assertThat(rows.get(c3.getName()).get("tag")).isEqualTo("release");
		assertThat(rows.get(c3.getName()).get("version")).isEqualTo("release");
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

		Path outputDir = wd.resolve("out");
		Path bcsCsv = outputDir.resolve("name-case-bcs.csv");
		Path cloneRoot = wd.resolve("clone");
		String url = remoteDir.toUri().toString();
		var exclusions = new io.github.alien.roseau.options.RoseauOptions.Exclude(List.of(".*\\.internal\\..*"), List.of());
		IncrementalWalkRepository.walk("name-case", url, cloneRoot.resolve(".git"),
			List.of(cloneRoot.resolve("src/main/java")), outputDir, exclusions);

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

		Path outputDir = wd.resolve("out");
		Path bcsCsv = outputDir.resolve("ann-fqn-case-bcs.csv");
		Path cloneRoot = wd.resolve("clone");
		String url = remoteDir.toUri().toString();
		var exclusions = new io.github.alien.roseau.options.RoseauOptions.Exclude(
			List.of(),
			List.of(new io.github.alien.roseau.options.RoseauOptions.AnnotationExclusion(
				"com.google.common.annotations.Beta", Map.of()))
		);
		IncrementalWalkRepository.walk("ann-fqn-case", url, cloneRoot.resolve(".git"),
			List.of(cloneRoot.resolve("src/main/java")), outputDir, exclusions);

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

		Path outputDir = wd.resolve("out");
		Path bcsCsv = outputDir.resolve("ann-simple-case-bcs.csv");
		Path cloneRoot = wd.resolve("clone");
		String url = remoteDir.toUri().toString();
		var exclusions = new io.github.alien.roseau.options.RoseauOptions.Exclude(
			List.of(),
			List.of(new io.github.alien.roseau.options.RoseauOptions.AnnotationExclusion("Internal", Map.of()))
		);
		IncrementalWalkRepository.walk("ann-simple-case", url, cloneRoot.resolve(".git"),
			List.of(cloneRoot.resolve("src/main/java")), outputDir, exclusions);

		List<Map<String, String>> rows = GitWalkTestUtils.readCsvRows(bcsCsv);
		assertThat(rows).isNotEmpty();
		assertThat(rows.stream().anyMatch(r ->
			r.get("commit").equals(breaking.getName()) &&
				Boolean.parseBoolean(r.get("is_excluded_symbol")))).isTrue();
	}
}
