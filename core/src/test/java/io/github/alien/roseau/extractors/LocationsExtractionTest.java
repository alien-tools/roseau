package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.util.stream.Stream;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertConstructor;
import static io.github.alien.roseau.utils.TestUtils.assertField;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static org.assertj.core.api.Assertions.assertThat;

class LocationsExtractionTest {
	@ParameterizedTest
	@EnumSource(value = ApiBuilderType.class, names = {"ASM"}, mode = EnumSource.Mode.EXCLUDE)
	void unified_source_locations(ApiBuilder builder) {
		var api = builder.build("""
			public class C1 {
				public int f;
				public void m() {}
				public void m(int i) {}
				public C1() {}
				public C1(int i) {}
				public static class C2 {
					public int f;
					public void m() {}
					public void m(int i) {}
					public C2() {}
					public C2(int i) {}
				}
			}""");

		var c1 = assertClass(api, "C1");
		var f1 = assertField(api, c1, "f");
		var m11 = assertMethod(api, c1, "m()");
		var m12 = assertMethod(api, c1, "m(int)");
		var cs11 = assertConstructor(api, c1, "<init>()");
		var cs12 = assertConstructor(api, c1, "<init>(int)");
		var c2 = assertClass(api, "C1$C2");
		var f2 = assertField(api, c2, "f");
		var m21 = assertMethod(api, c2, "m()");
		var m22 = assertMethod(api, c2, "m(int)");
		var cs21 = assertConstructor(api, c2, "<init>()");
		var cs22 = assertConstructor(api, c2, "<init>(int)");

		assertThat(c1.getLocation().file()).hasFileName("C1.java");
		assertThat(c1.getLocation().line()).isEqualTo(1);
		assertThat(f1.getLocation().file()).hasFileName("C1.java");
		assertThat(f1.getLocation().line()).isEqualTo(2);
		assertThat(m11.getLocation().file()).hasFileName("C1.java");
		assertThat(m11.getLocation().line()).isEqualTo(3);
		assertThat(m12.getLocation().file()).hasFileName("C1.java");
		assertThat(m12.getLocation().line()).isEqualTo(4);
		assertThat(cs11.getLocation().file()).hasFileName("C1.java");
		assertThat(cs11.getLocation().line()).isEqualTo(5);
		assertThat(cs12.getLocation().file()).hasFileName("C1.java");
		assertThat(cs12.getLocation().line()).isEqualTo(6);
		assertThat(c2.getLocation().file()).hasFileName("C1.java");
		assertThat(c2.getLocation().line()).isEqualTo(7);
		assertThat(f2.getLocation().file()).hasFileName("C1.java");
		assertThat(f2.getLocation().line()).isEqualTo(8);
		assertThat(m21.getLocation().file()).hasFileName("C1.java");
		assertThat(m21.getLocation().line()).isEqualTo(9);
		assertThat(m22.getLocation().file()).hasFileName("C1.java");
		assertThat(m22.getLocation().line()).isEqualTo(10);
		assertThat(cs21.getLocation().file()).hasFileName("C1.java");
		assertThat(cs21.getLocation().line()).isEqualTo(11);
		assertThat(cs22.getLocation().file()).hasFileName("C1.java");
		assertThat(cs22.getLocation().line()).isEqualTo(12);
	}

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
