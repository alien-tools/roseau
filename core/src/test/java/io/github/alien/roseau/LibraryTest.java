package io.github.alien.roseau;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.extractors.ExtractorType;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
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
	void exclude_types_and_members_by_name() {
		var sources = """
			module m { exports p.api; exports p.internal; }
			package p.internal;
			public class Excluded { public void m() {} }
			package p.api;
			public class C {
				public void m() {}
				public void excluded() {}
			}""";

		var exclude = new RoseauOptions.Exclude(List.of("p\\.internal\\..*", "p\\.api\\.C\\.excluded\\(\\)"), List.of());
		API api = TestUtils.buildSourcesAPI(sources, exclude);
		TypeDecl c = assertClass(api, "p.api.C");
		MethodDecl m = assertMethod(api, c, "m()");
		MethodDecl excludedMethod = assertMethod(api, c, "excluded()");
		TypeDecl excludedType = assertClass(api, "p.internal.Excluded");

		assertThat(api.isExcluded(c)).isFalse();
		assertThat(api.isExcluded(m)).isFalse();
		assertThat(api.isExcluded(excludedMethod)).isTrue();
		assertThat(api.isExcluded(excludedType)).isTrue();
	}

	@Test
	void name_exclusion_inherits() {
		var sources = """
			module m { exports p.api; }
			package p.api;
			public class Excluded {
				public static class Inner { public void m() {} }
				public void x() {}
			}""";

		var exclude = new RoseauOptions.Exclude(List.of("p\\.api\\.Excluded"), List.of());
		var api = TestUtils.buildSourcesAPI(sources, exclude);
		var excluded = assertClass(api, "p.api.Excluded");
		var inner = assertClass(api, "p.api.Excluded$Inner");
		var x = assertMethod(api, excluded, "x()");
		var m = assertMethod(api, inner, "m()");

		assertThat(api.isExcluded(excluded)).isTrue();
		assertThat(api.isExcluded(inner)).isTrue();
		assertThat(api.isExcluded(m)).isTrue();
		assertThat(api.isExcluded(x)).isTrue();
	}

	@Test
	void exclude_types_and_members_by_annotation() {
		var sources = """
			module m { exports p.api; }
			package p.annotations;
			public @interface Internal {}
			package p.api;
			public class A {
				@p.annotations.Internal public void excluded() {}
			}
			@p.annotations.Internal public class B { public void m() {} }""";

		var exclude = new RoseauOptions.Exclude(
			List.of(),
			List.of(new RoseauOptions.AnnotationExclusion("p.annotations.Internal", Map.of())));

		var api = TestUtils.buildSourcesAPI(sources, exclude);
		var a = assertClass(api, "p.api.A");
		var excluded = assertMethod(api, a, "excluded()");
		var b = assertClass(api, "p.api.B");
		var m = assertMethod(api, b, "m()");

		assertThat(api.isExcluded(a)).isFalse();
		assertThat(api.isExcluded(excluded)).isTrue();
		assertThat(api.isExcluded(b)).isTrue();
		assertThat(api.isExcluded(m)).isTrue();
	}

	@Test
	void exclude_annotation_values() {
		var sources = """
			module m { exports p.api; }
			package p.api;
			public @interface Internal { String level(); }
			public class C {
				@Internal(level = "alpha") public void alpha() {}
				@Internal(level = "beta") public void beta() {}
			}
			""";

		var excludes = new RoseauOptions.Exclude(
			List.of(),
			List.of(new RoseauOptions.AnnotationExclusion("p.api.Internal", Map.of("level", "alpha")))
		);

		var api = TestUtils.buildSourcesAPI(sources, excludes);

		var c = assertClass(api, "p.api.C");
		var alpha = assertMethod(api, c, "alpha()");
		var beta = assertMethod(api, c, "beta()");

		assertThat(api.isExcluded(alpha)).isTrue();
		assertThat(api.isExcluded(beta)).isFalse();
	}
}
