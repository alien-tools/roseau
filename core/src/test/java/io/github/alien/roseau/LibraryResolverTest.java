package io.github.alien.roseau;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LibraryResolverTest {
	private final Path validJar = Path.of("src/test/resources/api-showcase.jar");

	@Test
	void resolve_with_all_parameters_set(@TempDir Path tempDir) throws IOException {
		var pom = tempDir.resolve("pom.xml");
		var cp = tempDir.resolve("cp.jar");
		Files.createFile(pom);
		Files.createFile(cp);

		var lib = new LibraryResolver().resolve(validJar, List.of(cp), pom);

		assertThat(lib.location()).isEqualTo(validJar.toAbsolutePath().normalize());
		assertThat(lib.classpath()).contains(cp.toAbsolutePath().normalize());
	}

	@Test
	void resolve_merges_custom_and_pom() throws IOException {
		var cp = Files.createTempFile("roseau", ".jar");
		try {
			var lib = new LibraryResolver().resolve(validJar, List.of(cp), Path.of("pom.xml"));
			assertThat(lib.classpath())
				.contains(cp.toAbsolutePath().normalize())
				.hasSizeGreaterThan(10);
		} finally {
			Files.deleteIfExists(cp);
		}
	}

	@Test
	void resolve_invalid_pom_throws(@TempDir Path tempDir) throws IOException {
		var invalidPom = tempDir.resolve("pom.txt");
		Files.createFile(invalidPom);

		assertThatThrownBy(() -> new LibraryResolver().resolve(validJar, List.of(), invalidPom))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Invalid path to POM file");
	}

	@Test
	void resolve_unknown_pom_throws(@TempDir Path tempDir) {
		var invalidPom = tempDir.resolve("pom.xml");

		assertThatThrownBy(() -> new LibraryResolver().resolve(validJar, List.of(), invalidPom))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Invalid path to POM file");
	}
}
