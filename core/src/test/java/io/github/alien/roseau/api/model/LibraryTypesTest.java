package io.github.alien.roseau.api.model;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.extractors.MavenClasspathBuilder;
import io.github.alien.roseau.extractors.TypesExtractor;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import io.github.alien.roseau.utils.ApiTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class LibraryTypesTest {
	Library mockLibrary;

	@BeforeEach
	void setUp() {
		mockLibrary = mock(Library.class);
	}

	@Test
	void get_all_types() {
		var t1 = ApiTestFactory.newInterface("test.pkg.I1", AccessModifier.PUBLIC);
		var t2 = ApiTestFactory.newInterface("test.pkg.I2", AccessModifier.PACKAGE_PRIVATE);
		var lt = new LibraryTypes(mockLibrary, List.of(t1, t2));

		assertThat(lt.getAllTypes()).containsOnly(t1, t2);
	}

	@Test
	void find_type_exists() {
		var t1 = ApiTestFactory.newInterface("test.pkg.I1", AccessModifier.PUBLIC);
		var t2 = ApiTestFactory.newInterface("test.pkg.I2", AccessModifier.PACKAGE_PRIVATE);
		var lt = new LibraryTypes(mockLibrary, List.of(t1, t2));

		assertThat(lt.findType("test.pkg.I1")).hasValue(t1);
		assertThat(lt.findType("test.pkg.I2")).hasValue(t2);
	}

	@Test
	void find_type_absent() {
		var lt = new LibraryTypes(mockLibrary, List.of());

		assertThat(lt.findType("test.pkg.Unknown")).isEmpty();
	}

	@Test
	void find_type_unexpected_kind() {
		var t1 = ApiTestFactory.newInterface("test.pkg.I1", AccessModifier.PUBLIC);
		var lt = new LibraryTypes(mockLibrary, List.of(t1));
		var opt = lt.findType("test.pkg.I1", ClassDecl.class);

		assertThat(opt).isEmpty();
	}

	@Test
	void find_type_unexpected_kind_sub() {
		var t1 = ApiTestFactory.newRecord("test.pkg.R1", AccessModifier.PACKAGE_PRIVATE);
		var lt = new LibraryTypes(mockLibrary, List.of(t1));
		var opt = lt.findType("test.pkg.R1", ClassDecl.class);

		assertThat(opt).isPresent();
	}

	@Test
	void duplicate_types() {
		var t1 = ApiTestFactory.newInterface("test.pkg.I1", AccessModifier.PUBLIC);
		var t2 = ApiTestFactory.newInterface("test.pkg.I1", AccessModifier.PACKAGE_PRIVATE);
		assertThatIllegalArgumentException().isThrownBy(() -> new LibraryTypes(mockLibrary, List.of(t1, t2)));
	}

	@Test
	void json_round_trip() throws IOException {
		Path sources = Path.of("src/main/java");
		MavenClasspathBuilder builder = new MavenClasspathBuilder();
		List<Path> classpath = builder.buildClasspath(Path.of("."));
		Library library = Library.builder().location(sources).classpath(classpath).build();
		TypesExtractor extractor = new JdtTypesExtractor();
		LibraryTypes orig = extractor.extractTypes(library);

		Path json = Path.of("roundtrip.json");
		orig.writeJson(json);

		LibraryTypes res = LibraryTypes.fromJson(json);
		Files.delete(json);

		assertThat(res).isEqualTo(orig);
	}
}
