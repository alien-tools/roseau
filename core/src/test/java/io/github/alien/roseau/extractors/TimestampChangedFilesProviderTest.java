package io.github.alien.roseau.extractors;

import io.github.alien.roseau.extractors.incremental.ChangedFilesProvider;
import io.github.alien.roseau.extractors.incremental.TimestampChangedFilesProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimestampChangedFilesProviderTest {
	void touch(Path file) throws IOException {
		Files.setLastModifiedTime(file, FileTime.fromMillis(Instant.now().toEpochMilli()));
	}

	@Test
	void blank_state_returns_all_files(@TempDir Path wd) throws Exception {
		var a = wd.resolve("A.java");
		var b = wd.resolve("B.txt");
		Files.createFile(a);
		Files.createFile(b);

		var provider = new TimestampChangedFilesProvider(wd, Set.of());
		Thread.sleep(10);

		var changes = provider.getChangedFiles();
		assertEquals(Set.of(), changes.updatedFiles());
		assertEquals(Set.of(), changes.deletedFiles());
		assertEquals(Set.of(a), changes.createdFiles());
	}

	@Test
	void same_files_unchanged_returns_nothing(@TempDir Path wd) throws Exception {
		var a = wd.resolve("A.java");
		var b = wd.resolve("B.txt");
		Files.createFile(a);
		Files.createFile(b);

		var provider = new TimestampChangedFilesProvider(wd, Set.of(a));
		Thread.sleep(10);

		var changes = provider.getChangedFiles();
		assertTrue(changes.hasNoChanges());
	}

	@Test
	void updated_files_are_reported(@TempDir Path wd) throws Exception {
		var a = wd.resolve("A.java");
		var b = wd.resolve("B.txt");
		Files.createFile(a);
		Files.createFile(b);

		var provider = new TimestampChangedFilesProvider(wd, Set.of(a));
		Thread.sleep(10);

		touch(a);
		touch(b);

		var changes = provider.getChangedFiles();
		assertEquals(Set.of(a), changes.updatedFiles());
		assertEquals(Set.of(), changes.deletedFiles());
		assertEquals(Set.of(), changes.createdFiles());
	}

	@Test
	void deleted_files_are_reported(@TempDir Path wd) throws Exception {
		var a = wd.resolve("A.java");
		var b = wd.resolve("B.txt");
		Files.createFile(a);
		Files.createFile(b);

		var provider = new TimestampChangedFilesProvider(wd, Set.of(a));
		Thread.sleep(10);

		Files.delete(a);
		Files.delete(b);

		var changes = provider.getChangedFiles();
		assertEquals(Set.of(), changes.updatedFiles());
		assertEquals(Set.of(a), changes.deletedFiles());
		assertEquals(Set.of(), changes.createdFiles());
	}

	@Test
	void created_files_are_reported(@TempDir Path wd) throws Exception {
		var a = wd.resolve("A.java");
		var b = wd.resolve("B.txt");
		Files.createFile(a);
		Files.createFile(b);

		var provider = new TimestampChangedFilesProvider(wd, Set.of(a));
		Thread.sleep(10);

		var c = Files.createFile(wd.resolve("B.java"));

		var changes = provider.getChangedFiles();
		assertEquals(Set.of(), changes.updatedFiles());
		assertEquals(Set.of(), changes.deletedFiles());
		assertEquals(Set.of(c), changes.createdFiles());
	}

	@Test
	void unknown_sources_should_throw() {
		assertThrows(IllegalArgumentException.class,
			() -> new TimestampChangedFilesProvider(Paths.get("unknown"), Set.of()));
	}

	@Test
	void a_file_cannot_be_in_two_states() {
		var a = Paths.get("A.java");
		assertThrows(IllegalArgumentException.class,
			() -> new ChangedFilesProvider.ChangedFiles(Set.of(a), Set.of(), Set.of(a)));
	}
}
