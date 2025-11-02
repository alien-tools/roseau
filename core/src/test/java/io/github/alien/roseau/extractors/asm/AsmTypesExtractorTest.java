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
	AsmTypesExtractor extractor;

	@BeforeEach
	void setUp() {
		extractor = new AsmTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
	}

	@Test
	void regular_jar_extracts_types_and_no_module() {
		var jar = Path.of("src/test/resources/api-showcase.jar");

		var types = extractor.extractTypes(Library.of(jar));
		assertThat(types.getModule()).isEqualTo(ModuleDecl.UNNAMED_MODULE);
		assertThat(types.getAllTypes()).isNotEmpty();
		assertThat(types.findType("io.github.alien.roseau.APIShowcase")).isPresent();
	}

	@Test
	void module_jar_extracts_types_and_module() {
		var jar = Path.of("src/test/resources/one-module.jar");

		var types = extractor.extractTypes(Library.of(jar));
		assertThat(types.getModule()).isEqualTo(new ModuleDecl("m", Set.of("pkg")));
		assertThat(types.getAllTypes()).isNotEmpty();
		assertThat(types.findType("pkg.C")).isPresent();
	}

	@Test
	void multi_release_jar_prefers_versioned_classes() {
		var jar = Path.of("src/test/resources/multi-release.jar");

		var types = extractor.extractTypes(Library.of(jar));
		assertThat(types.getModule()).isEqualTo(ModuleDecl.UNNAMED_MODULE);
		assertThat(types.getAllTypes()).isNotEmpty();

		var c = types.findType("pkg.C");
		assertThat(c).isPresent();
		// Java 17 version adds a @Deprecated
		assertThat(c.get().getAnnotations()).isNotEmpty();
	}
}
