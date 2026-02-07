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

		Path regularCsv = wd.resolve("regular.csv");
		Path incrementalCsv = wd.resolve("incremental.csv");

		Path regularRoot = wd.resolve("regular-clone");
		Path incrementalRoot = wd.resolve("incremental-clone");
		String url = remoteDir.toUri().toString();
		WalkRepository.walk(url, regularRoot.resolve(".git"), List.of(regularRoot.resolve("src/main/java")), List.of(), regularCsv);
		IncrementalWalkRepository.walk(url, incrementalRoot.resolve(".git"), List.of(incrementalRoot.resolve("src/main/java")), incrementalCsv);

		Map<String, GitWalkTestUtils.CsvRow> regular = GitWalkTestUtils.readCsvRows(regularCsv);
		Map<String, GitWalkTestUtils.CsvRow> incremental = GitWalkTestUtils.readCsvRows(incrementalCsv);

		assertThat(incremental.keySet()).isEqualTo(regular.keySet());
		assertThat(regular).containsKeys(c2.getName(), c3.getName());
		assertThat(incremental).containsKeys(c2.getName(), c3.getName());

		for (String sha : regular.keySet()) {
			var r = regular.get(sha);
			var i = incremental.get(sha);
			assertThat(i.typesCount()).isEqualTo(r.typesCount());
			assertThat(i.methodsCount()).isEqualTo(r.methodsCount());
			assertThat(i.fieldsCount()).isEqualTo(r.fieldsCount());
			assertThat(i.breakingChangesCount()).isEqualTo(r.breakingChangesCount());
		}

		assertThat(regular.get(c2.getName()).breakingChangesCount()).isZero();
		assertThat(regular.get(c2.getName()).apiTime()).isZero();
		assertThat(regular.get(c3.getName()).breakingChangesCount()).isGreaterThan(0);
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

		Path regularCsv = wd.resolve("regular-root-switch.csv");
		Path incrementalCsv = wd.resolve("incremental-root-switch.csv");

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
		WalkRepository.walk(url, regularRoot.resolve(".git"), sourceRootsRegular, List.of(), regularCsv);
		IncrementalWalkRepository.walk(url, incrementalRoot.resolve(".git"), sourceRootsIncremental, incrementalCsv);

		Map<String, GitWalkTestUtils.CsvRow> regular = GitWalkTestUtils.readCsvRows(regularCsv);
		Map<String, GitWalkTestUtils.CsvRow> incremental = GitWalkTestUtils.readCsvRows(incrementalCsv);

		assertThat(regular).containsKeys(c2.getName(), c3.getName());
		assertThat(incremental).containsKeys(c2.getName(), c3.getName());

		assertThat(incremental.keySet()).isEqualTo(regular.keySet());
		for (String sha : regular.keySet()) {
			var r = regular.get(sha);
			var i = incremental.get(sha);
			assertThat(i.typesCount()).isEqualTo(r.typesCount());
			assertThat(i.methodsCount()).isEqualTo(r.methodsCount());
			assertThat(i.fieldsCount()).isEqualTo(r.fieldsCount());
			assertThat(i.breakingChangesCount()).isEqualTo(r.breakingChangesCount());
		}
	}
}
