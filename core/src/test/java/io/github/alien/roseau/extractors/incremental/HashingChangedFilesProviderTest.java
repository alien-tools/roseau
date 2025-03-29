package io.github.alien.roseau.extractors.incremental;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HashingChangedFilesProviderTest {
	@TempDir
	Path left;
	@TempDir
	Path right;
	HashingChangedFilesProvider provider;

	@BeforeEach
	void setUp() throws IOException {
		provider = new HashingChangedFilesProvider(HashFunction.XXHASH);
		for (Path wd : List.of(left, right)) {
			Files.writeString(wd.resolve("A.java"), "abc");
			Files.writeString(wd.resolve("B.java"), "def");
			Files.writeString(wd.resolve("C.java"), "ghi");
			Files.writeString(wd.resolve("D.java"), "ghi");
			Files.writeString(wd.resolve("E.txt"), "jkl");
		}
	}

	@Test
	void no_changes_for_identical_directories() {
		var result = provider.getChangedFiles(left, right);
		assertThat(result).isEqualTo(ChangedFiles.NO_CHANGES);
	}

	@Test
	void updated_files() throws IOException {
		Files.writeString(right.resolve("A.java"), "-");
		Files.writeString(right.resolve("C.java"), "-");
		Files.writeString(right.resolve("E.txt"), "-");

		var result = provider.getChangedFiles(left, right);

		var changed = Set.of(left.resolve("A.java"), left.resolve("C.java"));
		assertThat(result).isEqualTo(new ChangedFiles(changed, Set.of(), Set.of()));
	}

	@Test
	void deleted_files() throws IOException {
		Files.deleteIfExists(right.resolve("A.java"));
		Files.deleteIfExists(right.resolve("C.java"));
		Files.deleteIfExists(right.resolve("E.txt"));

		var result = provider.getChangedFiles(left, right);

		var deleted = Set.of(left.resolve("A.java"), left.resolve("C.java"));
		assertThat(result).isEqualTo(new ChangedFiles(Set.of(), deleted, Set.of()));
	}

	@Test
	void created_files() throws IOException {
		Files.writeString(right.resolve("F.java"), "-");
		Files.createDirectory(right.resolve("new"));
		Files.writeString(right.resolve("new/G.java"), "-");
		Files.writeString(right.resolve("new/H.txt"), "-");

		var result = provider.getChangedFiles(left, right);

		var created = Set.of(right.resolve("F.java"), right.resolve("new/G.java"));
		assertThat(result).isEqualTo(new ChangedFiles(Set.of(), Set.of(), created));
	}

	@Test
	void change_existing_hash() throws IOException {
		Files.writeString(right.resolve("A.java"), "ghi");

		var result = provider.getChangedFiles(left, right);

		var changed = Set.of(left.resolve("A.java"));
		assertThat(result).isEqualTo(new ChangedFiles(changed, Set.of(), Set.of()));
	}

	@Test
	void move_file() throws IOException {
		Files.createDirectory(right.resolve("new"));
		Files.move(right.resolve("A.java"), right.resolve("new/A.java"));

		var result = provider.getChangedFiles(left, right);

		var deleted = Set.of(left.resolve("A.java"));
		var created = Set.of(right.resolve("new/A.java"));
		assertThat(result).isEqualTo(new ChangedFiles(Set.of(), deleted, created));
	}
}
