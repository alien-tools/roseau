package io.github.alien.roseau.extractors.asm;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AsmTypesExtractorTest {
	private static final Path NORMAL_JAR = Path.of("src/test/resources/api-showcase.jar");
	private static final Path ONE_MODULE_JAR = Path.of("src/test/resources/one-module.jar");
	private static final Path MULTI_RELEASE_JAR = Path.of("src/test/resources/multi-release.jar");
	private static final Path CORRUPT_JAR = Path.of("src/test/resources/corrupt.jar");
	private static final Path UNSUPPORTED_VERSION_JAR = Path.of("src/test/resources/unsupported-version.jar");

	AsmTypesExtractor extractor;

	@BeforeEach
	void setUp() {
		extractor = new AsmTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
	}

	@Test
	void regular_jar_extracts_types_and_no_module() {
		var types = extractor.extractTypes(Library.of(NORMAL_JAR));
		assertThat(types.getModule()).isEqualTo(ModuleDecl.UNNAMED_MODULE);
		assertThat(types.getAllTypes()).isNotEmpty();
		assertThat(types.findType("io.github.alien.roseau.APIShowcase")).isPresent();
	}

	@Test
	void module_jar_extracts_types_and_module() {
		var types = extractor.extractTypes(Library.of(ONE_MODULE_JAR));
		assertThat(types.getModule()).isEqualTo(new ModuleDecl("m", Set.of("pkg")));
		assertThat(types.getAllTypes()).isNotEmpty();
		assertThat(types.findType("pkg.C")).isPresent();
	}

	@Test
	void multi_release_jar_prefers_versioned_classes() {
		var types = extractor.extractTypes(Library.of(MULTI_RELEASE_JAR));
		assertThat(types.getModule()).isEqualTo(ModuleDecl.UNNAMED_MODULE);
		assertThat(types.getAllTypes()).isNotEmpty();

		var c = types.findType("pkg.C");
		assertThat(c).isPresent();
		// Java 17 version adds a @Deprecated
		assertThat(c.get().getAnnotations()).isNotEmpty();
	}

	@Test
	void corrupt_class_file_does_not_abort_whole_jar() {
		var types = extractor.extractTypes(Library.of(CORRUPT_JAR));
		assertThat(types.findType("pkg.Valid")).isPresent();
	}

	@Test
	void unsupported_class_file_version_does_not_abort_whole_jar() {
		var types = extractor.extractTypes(Library.of(UNSUPPORTED_VERSION_JAR));
		assertThat(types.findType("pkg.Valid")).isPresent();
	}
}
