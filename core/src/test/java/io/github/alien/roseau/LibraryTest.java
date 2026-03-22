package io.github.alien.roseau;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LibraryTest {
	private final Path validJar = Path.of("src/test/resources/api-showcase.jar");

	@Test
	void of_jar_uses_absolute_location() {
		var lib = Library.of(validJar);
		assertThat(lib.location()).isEqualTo(validJar.toAbsolutePath().normalize());
		assertThat(lib.isJar()).isTrue();
		assertThat(lib.isSources()).isFalse();
	}

	@Test
	void of_sources_uses_absolute_location(@TempDir Path tempDir) {
		var lib = Library.of(tempDir);
		assertThat(lib.location()).isEqualTo(tempDir.toAbsolutePath().normalize());
		assertThat(lib.isSources()).isTrue();
		assertThat(lib.isJar()).isFalse();
	}

	@Test
	void of_module_info_normalizes_to_parent(@TempDir Path tempDir) throws IOException {
		var module = tempDir.resolve("module-info.java");
		Files.createFile(module);

		var lib = Library.of(module);
		assertThat(lib.location()).isEqualTo(tempDir.toAbsolutePath().normalize());
		assertThat(lib.isSources()).isTrue();
		assertThat(lib.isJar()).isFalse();
	}

	@Test
	void of_unknown_throws() {
		assertThatThrownBy(() -> Library.of(Path.of("unknown/path")))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Invalid path to library");
	}

	@Test
	void of_invalid_jar_throws() {
		assertThatThrownBy(() -> Library.of(Path.of("src/test/resources/invalid.jar")))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Invalid path to library");
	}

	@Test
	void multiple_module_info_throws(@TempDir Path tempDir) throws IOException {
		var pkg1 = tempDir.resolve("pkg1");
		var pkg2 = tempDir.resolve("pkg2");
		Files.createDirectories(pkg1);
		Files.createDirectories(pkg2);
		Files.createFile(pkg1.resolve("module-info.java"));
		Files.createFile(pkg2.resolve("module-info.java"));

		assertThatThrownBy(() -> Library.of(tempDir))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("A library cannot contain multiple module-info.java");
	}
}
