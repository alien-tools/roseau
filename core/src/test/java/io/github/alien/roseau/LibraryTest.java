package io.github.alien.roseau;

import io.github.alien.roseau.extractors.ExtractorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LibraryTest {
	@Test
	void of_jar_defaults_to_asm(@TempDir Path tempDir) throws IOException {
		var tmp = tempDir.resolve("lib.jar");
		Files.createFile(tmp);

		var lib = Library.of(tmp);
		assertThat(lib.isJar()).isTrue();
		assertThat(lib.isSources()).isFalse();
		assertThat(lib.getExtractorType()).isEqualTo(ExtractorType.ASM);
	}

	@Test
	void of_sources_defaults_to_jdt(@TempDir Path tempDir) throws IOException {
		var dir = tempDir.resolve("src");
		Files.createDirectories(dir);

		var lib = Library.of(dir);
		assertThat(lib.isSources()).isTrue();
		assertThat(lib.isJar()).isFalse();
		assertThat(lib.getExtractorType()).isEqualTo(ExtractorType.JDT);
	}

	@Test
	void of_unknown_throws() {
		assertThatThrownBy(() -> Library.of(Path.of("unknown/path")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid path to library");
	}

	@Test
	void builder_without_location_throws() {
		assertThatThrownBy(() -> Library.builder().build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid path to library");
	}

	@Test
	void builder_with_all_parameters_set(@TempDir Path tempDir) throws IOException {
		var jar = tempDir.resolve("lib.jar");
		var pom = tempDir.resolve("pom.xml");
		var cp = List.of(tempDir.resolve("cp"));
		Files.createFile(jar);
		Files.createFile(pom);

		var lib = Library.builder()
			.location(jar)
			.classpath(cp)
			.pom(pom)
			.extractorType(ExtractorType.ASM)
			.build();

		assertThat(lib.getLocation()).isEqualTo(jar.toAbsolutePath());
		assertThat(lib.getClasspath()).isEqualTo(cp);
		assertThat(lib.getPom()).isEqualTo(pom);
		assertThat(lib.getExtractorType()).isEqualTo(ExtractorType.ASM);
	}

	@Test
	void builder_with_spoon_extractor(@TempDir Path tempDir) throws IOException {
		var dir = tempDir.resolve("src");
		Files.createDirectories(dir);

		var lib = Library.builder()
			.location(dir)
			.extractorType(ExtractorType.SPOON)
			.build();

		assertThat(lib.isSources()).isTrue();
		assertThat(lib.getExtractorType()).isEqualTo(ExtractorType.SPOON);
	}

	@Test
	void builder_invalid_location_throws() {
		var nonExisting = Path.of("unknown/path");
		assertThatThrownBy(() -> Library.builder().location(nonExisting).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid path to library");
	}

	@Test
	void builder_invalid_pom_throws(@TempDir Path tempDir) throws IOException {
		var dir = tempDir.resolve("src");
		Files.createDirectories(dir);
		var invalidPom = tempDir.resolve("pom.txt");
		Files.createFile(invalidPom);

		assertThatThrownBy(() -> Library.builder().location(dir).pom(invalidPom).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid path to POM file");
	}

	@Test
	void builder_unknown_pom_throws(@TempDir Path tempDir) throws IOException {
		var dir = tempDir.resolve("src");
		Files.createDirectories(dir);
		var invalidPom = tempDir.resolve("pom.xml");

		assertThatThrownBy(() -> Library.builder().location(dir).pom(invalidPom).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid path to POM file");
	}

	@Test
	void builder_invalid_extractor_throws(@TempDir Path tempDir) throws IOException {
		var sources = tempDir.resolve("src");
		Files.createDirectories(sources);
		var jar = tempDir.resolve("lib.jar");
		Files.createFile(jar);

		assertThatThrownBy(() -> Library.builder().location(sources).extractorType(ExtractorType.ASM).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("ASM extractor cannot be used on source directories");

		assertThatThrownBy(() -> Library.builder().location(jar).extractorType(ExtractorType.JDT).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Source extractors cannot be used on JARs");

		assertThatThrownBy(() -> Library.builder().location(jar).extractorType(ExtractorType.SPOON).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Source extractors cannot be used on JARs");
	}
}
