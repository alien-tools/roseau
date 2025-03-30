package io.github.alien.roseau.extractors.jdt;

import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdtTypesExtractorTest {
	@TempDir
	Path wd;

	@Test
	void parse_null_throws() {
		var extractor = new JdtTypesExtractor();
		assertThrows(NullPointerException.class, () -> extractor.extractTypes(null));
	}

	@Test
	void parse_invalid_location_throws() {
		var extractor = new JdtTypesExtractor();
		assertThrows(RoseauException.class, () -> extractor.extractTypes(Path.of("invalid")));
	}

	@Test
	void parse_empty_sources_empty_api() {
		var extractor = new JdtTypesExtractor();
		var api = extractor.extractTypes(wd);

		assertThat(api).isNotNull();
		assertThat(api.getAllTypes()).isEmpty();
	}

	@Test
	void parse_valid_file_creates_api() throws Exception {
		Files.writeString(wd.resolve("A.java"), """
			package pkg;
			public class A {}""");

		var extractor = new JdtTypesExtractor();
		var api = extractor.extractTypes(wd);

		assertThat(api.getAllTypes())
			.singleElement()
			.extracting(TypeDecl::getQualifiedName)
			.isEqualTo("pkg.A");
	}

	@Test
	void parse_syntax_error_ignores() throws Exception {
		Files.writeString(wd.resolve("A.java"), """
			package pkg;
			public clazz A {}""");
		Files.writeString(wd.resolve("B.java"), """
			package pkg;
			public class B {
				public int float double f;
			}""");

		var extractor = new JdtTypesExtractor();
		var api = extractor.extractTypes(wd);

		assertThat(api.getAllTypes()).hasSize(1);

		var b = api.findType("pkg.B").orElseThrow();
		assertThat(b.getDeclaredFields()).isEmpty();
	}

	@Test
	void unresolved_symbols_have_fqns() throws Exception {
		Files.writeString(wd.resolve("A.java"), """
			package pkg;
			import unknown.B;
			import unknown.D;
			import java.util.List;
			public class A<T> extends unknown.A implements B {
				public List<unknown.C> f;
				public D m(unknown.E[] p1, F p2) { return null; }
				public <U> B<U> n(unknown.C<U> p1, B<T> p2) { return null; }
			}""");

		var extractor = new JdtTypesExtractor();
		var types = extractor.extractTypes(wd);

		assertThat(types.getAllTypes()).hasSize(1);

		var cls = assertClass(types.toAPI(), "pkg.A");
		assertThat(cls.getSuperClass()).isEqualTo(new TypeReference<>("unknown.A"));
		assertThat(cls.getImplementedInterfaces().getFirst()).isEqualTo(new TypeReference<>("unknown.B"));
		assertThat(cls.getDeclaredFields().getFirst().getType()).isEqualTo(
			new TypeReference<>("java.util.List", List.of(new TypeReference<>("unknown.C"))));
		assertThat(cls.getDeclaredMethods().getFirst().getType()).isEqualTo(new TypeReference<>("unknown.D"));
		assertThat(types.toAPI().getErasure(cls.getDeclaredMethods().getFirst())).isEqualTo("m(unknown.E[],pkg.F)");

		// JDT can't parse public <U> B<U> n(unknown.C<U> p1, B<T> p2)
		assertThat(cls.getDeclaredMethods()).hasSize(1);
	}

	@Test
	void resolved_symbols_from_src_root() throws Exception {
		var unknown = Files.createDirectories(wd.resolve("unknown"));
		Files.writeString(unknown.resolve("A.java"), """
			package unknown;
			public class A {}""");
		Files.writeString(unknown.resolve("B.java"), """
			package unknown;
			public interface B<T> {}""");
		Files.writeString(unknown.resolve("C.java"), """
			package unknown;
			public class C<T> {}""");
		Files.writeString(unknown.resolve("D.java"), """
			package unknown;
			public class D {}""");
		Files.writeString(unknown.resolve("E.java"), """
			package unknown;
			public class E {}""");
		Files.writeString(wd.resolve("F.java"), """
			package pkg;
			public class F {}""");
		Files.writeString(wd.resolve("A.java"), """
			package pkg;
			import unknown.B;
			import unknown.D;
			import java.util.List;
			public class A<T> extends unknown.A implements B {
				public List<unknown.C> f;
				public D m(unknown.E[] p1, F p2) { return null; }
				public <U> B<U> n(unknown.C<U> p1, B<T> p2) { return null; }
			}""");

		var extractor = new JdtTypesExtractor();
		var types = extractor.extractTypes(wd);

		assertThat(types.getAllTypes()).hasSize(7);

		var cls = assertClass(types.toAPI(), "pkg.A");
		assertThat(cls.getSuperClass()).isEqualTo(new TypeReference<>("unknown.A"));
		assertThat(cls.getImplementedInterfaces().getFirst()).isEqualTo(new TypeReference<>("unknown.B"));
		assertThat(cls.getDeclaredFields().getFirst().getType()).isEqualTo(
			new TypeReference<>("java.util.List", List.of(new TypeReference<>("unknown.C"))));
		assertThat(cls.getDeclaredMethods().getFirst().getType()).isEqualTo(new TypeReference<>("unknown.D"));
		assertThat(types.toAPI().getErasure(cls.getDeclaredMethods().getFirst())).isEqualTo("m(unknown.E[],pkg.F)");
		assertThat(cls.getDeclaredMethods().get(1).getType()).isEqualTo(
			new TypeReference<>("unknown.B", List.of(new TypeParameterReference("U"))));
		assertThat(cls.getDeclaredMethods().get(1).getParameters().getFirst().type()).isEqualTo(
			new TypeReference<>("unknown.C", List.of(new TypeParameterReference("U"))));
		assertThat(cls.getDeclaredMethods().get(1).getParameters().get(1).type()).isEqualTo(
			new TypeReference<>("unknown.B", List.of(new TypeParameterReference("T"))));
	}
}
