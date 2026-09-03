package io.github.alien.roseau.extractors.jdt;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static org.assertj.core.api.Assertions.assertThat;

class JdtTypesExtractorTest {
	JdtTypesExtractor extractor;

	@BeforeEach
	void setUp() {
		extractor = new JdtTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
	}

	@TempDir
	Path wd;

	@Test
	void parse_empty_sources_empty_api() {
		var api = extractor.extractTypes(Library.of(wd));

		assertThat(api).isNotNull();
		assertThat(api.getAllTypes()).isEmpty();
	}

	@Test
	void parse_valid_file_creates_api() throws IOException {
		Files.writeString(wd.resolve("A.java"), """
			package pkg;
			public class A {}""");

		var api = extractor.extractTypes(Library.of(wd));

		assertThat(api.getAllTypes())
			.singleElement()
			.extracting(TypeDecl::getQualifiedName)
			.isEqualTo("pkg.A");
	}

	@Test
	void parse_syntax_error_ignores() throws IOException {
		Files.writeString(wd.resolve("A.java"), """
			package pkg;
			public clazz A {}""");
		Files.writeString(wd.resolve("B.java"), """
			package pkg;
			public class B {
				public int float double f;
			}""");

		var api = extractor.extractTypes(Library.of(wd));

		assertThat(api).isNotNull();
		assertThat(api.getAllTypes()).hasSize(1);

		var b = api.findType("pkg.B").orElseThrow();
		assertThat(b.getDeclaredFields()).isEmpty();
	}

	@Test
	void unresolved_symbols_have_fqns() throws IOException {
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

		var types = extractor.extractTypes(Library.of(wd));
		var api = Roseau.buildAPI(types);

		assertThat(types).isNotNull();
		assertThat(types.getAllTypes()).hasSize(1);

		var cls = assertClass(api, "pkg.A");
		assertThat(cls.getSuperClass()).isEqualTo(new TypeReference<>("unknown.A"));
		assertThat(cls.getImplementedInterfaces().iterator().next()).isEqualTo(new TypeReference<>("unknown.B"));
		assertThat(cls.getDeclaredFields().iterator().next().getType()).isEqualTo(
			new TypeReference<>("java.util.List", List.of(new TypeReference<>("unknown.C"))));
		assertThat(cls.getDeclaredMethods().iterator().next().getType()).isEqualTo(new TypeReference<>("unknown.D"));
		assertThat(api.analyzer().getErasure(cls.getDeclaredMethods().iterator().next())).isEqualTo("m(unknown.E[],F)");

		// JDT can't parse public <U> B<U> n(unknown.C<U> p1, B<T> p2)
		assertThat(cls.getDeclaredMethods()).hasSize(1);
	}

	@Test
	void resolved_symbols_from_src_root() throws IOException {
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
			public class A<T> extends unknown.A implements B<T> {
				public List<unknown.C> f;
				public D m(unknown.E[] p1, F p2) { return null; }
				public <U> B<U> n(unknown.C<U> p1, B<T> p2) { return null; }
			}""");

		var types = extractor.extractTypes(Library.of(wd));
		var api = Roseau.buildAPI(types);

		assertThat(types.getAllTypes()).hasSize(7);

		var cls = assertClass(api, "pkg.A");
		assertThat(cls.getSuperClass()).isEqualTo(new TypeReference<>("unknown.A"));
		assertThat(cls.getImplementedInterfaces().iterator().next()).isEqualTo(
			new TypeReference<>("unknown.B", List.of(new TypeParameterReference("T"))));
		assertThat(cls.getDeclaredFields().iterator().next().getType()).isEqualTo(
			new TypeReference<>("java.util.List", List.of(new TypeReference<>("unknown.C"))));
		var m = cls.getDeclaredMethods().stream().filter(mt -> "m".equals(mt.getSimpleName())).findFirst().get();
		var n = cls.getDeclaredMethods().stream().filter(mt -> "n".equals(mt.getSimpleName())).findFirst().get();
		assertThat(m.getType()).isEqualTo(new TypeReference<>("unknown.D"));
		assertThat(api.analyzer().getErasure(m)).isEqualTo("m(unknown.E[],pkg.F)");
		assertThat(n.getType()).isEqualTo(
			new TypeReference<>("unknown.B", List.of(new TypeParameterReference("U"))));
		assertThat(n.getParameters().getFirst().type()).isEqualTo(
			new TypeReference<>("unknown.C", List.of(new TypeParameterReference("U"))));
		assertThat(n.getParameters().get(1).type()).isEqualTo(
			new TypeReference<>("unknown.B", List.of(new TypeParameterReference("T"))));
	}

	@Test
	void on_demand_import_is_preferred_over_the_current_package() throws IOException {
		assertThat(typeOfField("""
			package pkg;
			import com.example.absent.*;
			public class A {
				public Missing f;
			}""")).isEqualTo(new TypeReference<>("com.example.absent.Missing"));
	}

	@Test
	void single_type_import_shadows_on_demand_import() throws IOException {
		assertThat(typeOfField("""
			package pkg;
			import com.example.other.*;
			import com.example.exact.Missing;
			public class A {
				public Missing f;
			}""")).isEqualTo(new TypeReference<>("com.example.exact.Missing"));
	}

	@Test
	void resolvable_on_demand_import_is_not_a_candidate() throws IOException {
		assertThat(typeOfField("""
			package pkg;
			import java.util.*;
			import com.example.absent.*;
			public class A {
				public Missing f;
			}""")).isEqualTo(new TypeReference<>("com.example.absent.Missing"));
	}

	@Test
	void ambiguous_on_demand_imports_keep_the_simple_name() throws IOException {
		assertThat(typeOfField("""
			package pkg;
			import com.example.absent.*;
			import com.example.gone.*;
			public class A {
				public Missing f;
			}""")).isEqualTo(new TypeReference<>("Missing"));
	}

	@Test
	void qualified_name_sharing_a_prefix_with_the_package_is_preserved() throws IOException {
		assertThat(typeOfField("""
			package pkg;
			public class A {
				public pkgtwo.Missing f;
			}""")).isEqualTo(new TypeReference<>("pkgtwo.Missing"));
	}

	@Test
	void type_arguments_are_resolved_too() throws IOException {
		assertThat(typeOfField("""
			package pkg;
			import java.util.List;
			import com.example.absent.*;
			public class A {
				public List<Missing> f;
			}""")).isEqualTo(new TypeReference<>("java.util.List",
			List.of(new TypeReference<>("com.example.absent.Missing"))));
	}

	@Test
	void unresolved_generic_type_keeps_its_type_arguments() throws IOException {
		assertThat(typeOfField("""
			package pkg;
			import com.example.absent.*;
			public class A {
				public Missing<String> f;
			}""")).isEqualTo(new TypeReference<>("com.example.absent.Missing", List.of(TypeReference.STRING)));
	}

	private ITypeReference typeOfField(String source) throws IOException {
		Files.writeString(wd.resolve("A.java"), source);
		var api = Roseau.buildAPI(extractor.extractTypes(Library.of(wd)));
		return assertClass(api, "pkg.A").getDeclaredFields().iterator().next().getType();
	}
}
