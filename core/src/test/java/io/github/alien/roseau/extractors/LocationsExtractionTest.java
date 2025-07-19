package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LocationsExtractionTest {
	@Test
	void accurate_asm_locations() {
		var jar = Path.of("src/test/resources/api-showcase.jar");
		var api = new AsmTypesExtractor().extractTypes(jar).toAPI();

		api.getLibraryTypes().getAllTypes().forEach(t -> {
			assertThat(t.getLocation().file()).hasFileName("APIShowcase.java");
			assertThat(t.getLocation().line()).isEqualTo(-1);
			assertThat(t.getDeclaredFields()).extracting(Symbol::getLocation).allSatisfy(loc -> {
				assertThat(loc.file()).hasFileName("APIShowcase.java");
				assertThat(loc.line()).isEqualTo(-1);
			});
			assertThat(t.getDeclaredMethods().stream().filter(m -> !m.isAbstract() && !m.isNative()))
				.extracting(Symbol::getLocation)
				.allSatisfy(loc -> {
					assertThat(loc.file()).hasFileName("APIShowcase.java");
					assertThat(loc.line()).isGreaterThan(0);
				});
		});
	}

	@Test
	void no_jar_locations_when_no_debug_information() {
		var jar = Path.of("src/test/resources/api-showcase-no-debug.jar");
		var api = new AsmTypesExtractor().extractTypes(jar).toAPI();

		var locations = api.getLibraryTypes().getAllTypes().stream()
			.flatMap(type -> Stream.concat(Stream.of(type.getLocation()), Stream.concat(
				type.getDeclaredFields().stream().map(FieldDecl::getLocation),
				type.getDeclaredMethods().stream().map(MethodDecl::getLocation))
			))
			.toList();

		assertThat(locations).containsOnly(SourceLocation.NO_LOCATION);
	}
}
