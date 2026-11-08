package io.github.alien.roseau;

import io.github.alien.roseau.extractors.ExtractorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LibraryTest {
	final Path validJar = Path.of("src/test/resources/api-showcase.jar");

	@Test
	void of_jar_defaults_to_asm() {
		var lib = Library.of(validJar);
		assertThat(lib.getLocation()).isEqualTo(validJar.toAbsolutePath());
		assertThat(lib.isJar()).isTrue();
		assertThat(lib.isSources()).isFalse();
		assertThat(lib.getExtractorType()).isEqualTo(ExtractorType.ASM);
	}

	@Test
	void of_sources_defaults_to_jdt(@TempDir Path tempDir) throws IOException {
		var src = tempDir.resolve("src");
		Files.createDirectories(src);

		var lib = Library.of(tempDir);
		assertThat(lib.getLocation()).isEqualTo(tempDir);
		assertThat(lib.isSources()).isTrue();
		assertThat(lib.isJar()).isFalse();
		assertThat(lib.getExtractorType()).isEqualTo(ExtractorType.JDT);
	}

	@Test
	void of_module_info_defaults_to_jdt(@TempDir Path tempDir) throws IOException {
		var module = tempDir.resolve("module-info.java");
		Files.createFile(module);

		var lib = Library.of(module);
		assertThat(lib.getLocation()).isEqualTo(tempDir);
		assertThat(lib.isSources()).isTrue();
		assertThat(lib.isJar()).isFalse();
		assertThat(lib.getExtractorType()).isEqualTo(ExtractorType.JDT);
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

	@Test
	void builder_without_location_throws() {
		assertThatThrownBy(() -> Library.builder().build())
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Invalid path to library");
	}

	@Test
	void builder_with_all_parameters_set(@TempDir Path tempDir) throws IOException {
		var pom = tempDir.resolve("pom.xml");
		var cp = List.of(tempDir.resolve("cp"));
		Files.createFile(pom);

		var lib = Library.builder()
			.location(validJar)
			.classpath(cp)
			.pom(pom)
			.build();

		assertThat(lib.getLocation()).isEqualTo(validJar.toAbsolutePath());
		assertThat(lib.getCustomClasspath()).isEqualTo(cp);
		assertThat(lib.getPom()).isEqualTo(pom);
		assertThat(lib.getExtractorType()).isEqualTo(ExtractorType.ASM);
	}

	@Test
	void classpath_merges_custom_and_pom() {
		var pom = Path.of("pom.xml"); // Roseau's pom.xml
		var cp = List.of(Path.of("cp"));

		var lib = Library.builder()
			.location(validJar)
			.classpath(cp)
			.pom(pom)
			.build();

		assertThat(lib.getLocation()).isEqualTo(validJar.toAbsolutePath());
		assertThat(lib.getCustomClasspath()).isEqualTo(cp);
		assertThat(lib.getPom()).isEqualTo(pom);
		assertThat(lib.getExtractorType()).isEqualTo(ExtractorType.ASM);
		assertThat(lib.getClasspath()).hasSizeGreaterThan(10);
	}

	@Test
	void classpath_accepts_class_directories(@TempDir Path tempDir) throws IOException {
		var dependency = tempDir.resolve("Base.java");
		Files.writeString(dependency, "package dependency; public class Base { public void inherited() {} }");
		var classes = Files.createDirectory(tempDir.resolve("classes"));
		assertThat(ToolProvider.getSystemJavaCompiler().run(null, null, null,
			"-proc:none", "-d", classes.toString(), dependency.toString())).isZero();
		var sources = Files.createDirectory(tempDir.resolve("sources"));

		var lib = Library.builder()
			.location(sources)
			.classpath(List.of(classes))
			.build();

		assertThat(lib.getClasspath()).containsExactly(classes);
	}

	@Test
	void builder_invalid_location_throws() {
		var nonExisting = Path.of("unknown/path");
		assertThatThrownBy(() -> Library.builder().location(nonExisting).build())
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Invalid path to library");
	}

	@Test
	void builder_invalid_pom_throws(@TempDir Path tempDir) throws IOException {
		var dir = tempDir.resolve("src");
		Files.createDirectories(dir);
		var invalidPom = tempDir.resolve("pom.txt");
		Files.createFile(invalidPom);

		assertThatThrownBy(() -> Library.builder().location(dir).pom(invalidPom).build())
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Invalid path to POM file");
	}

	@Test
	void builder_unknown_pom_throws(@TempDir Path tempDir) throws IOException {
		var dir = tempDir.resolve("src");
		Files.createDirectories(dir);
		var invalidPom = tempDir.resolve("pom.xml");

		assertThatThrownBy(() -> Library.builder().location(dir).pom(invalidPom).build())
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Invalid path to POM file");
	}

	@Test
	void java_release_defaults_to_the_host() {
		var lib = Library.of(validJar);

		assertThat(lib.getJavaRelease()).isEqualTo(JavaRelease.host());
		assertThat(lib.isModuleDeclarationIgnored()).isFalse();
	}

	@Test
	void jar_can_be_checked_on_another_release() {
		var lib = Library.builder().location(validJar).javaRelease(17).build();

		assertThat(lib.getJavaRelease()).isEqualTo(17);
		assertThat(lib.isModuleDeclarationIgnored()).isFalse();
	}

	@Test
	void module_declaration_is_ignored_below_java_9() {
		var lib = Library.builder().location(validJar).javaRelease(8).build();

		assertThat(lib.isIgnoringModule()).isFalse();
		assertThat(lib.isModuleDeclarationIgnored()).isTrue();
	}

	@Test
	void module_declaration_can_be_ignored_on_purpose() {
		var lib = Library.builder().location(validJar).ignoreModule(true).javaRelease(11).build();

		assertThat(lib.isIgnoringModule()).isTrue();
		assertThat(lib.isModuleDeclarationIgnored()).isTrue();
	}

	@Test
	void unsupported_java_release_throws() {
		assertThatThrownBy(() -> Library.builder().location(validJar).javaRelease(7).build())
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Unsupported Java release 7");
		assertThatThrownBy(() -> Library.builder().location(validJar).javaRelease(JavaRelease.host() + 1).build())
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Unsupported Java release " + (JavaRelease.host() + 1));
	}

	@Test
	void sources_can_only_be_checked_on_the_host_release(@TempDir Path tempDir) {
		assertThatThrownBy(() -> Library.builder().location(tempDir).javaRelease(17).build())
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Source libraries can only be checked on the release of the JDK running Roseau (" +
				JavaRelease.host() + ")")
			.hasMessageContaining("compare compiled JARs instead");
		assertThat(Library.builder().location(tempDir).javaRelease(JavaRelease.host()).build().getJavaRelease())
			.isEqualTo(JavaRelease.host());
	}

	@Test
	void java_release_is_part_of_equality() {
		var java17 = Library.builder().location(validJar).javaRelease(17).build();
		var java21 = Library.builder().location(validJar).javaRelease(21).build();

		assertThat(java17).isNotEqualTo(java21);
		assertThat(java17).isEqualTo(Library.builder().location(validJar).javaRelease(17).build());
		assertThat(java17.toString()).contains("javaRelease=17");
	}
}
