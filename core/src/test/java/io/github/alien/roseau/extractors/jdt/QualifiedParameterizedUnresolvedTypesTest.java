package io.github.alien.roseau.extractors.jdt;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.API;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertConstructor;
import static io.github.alien.roseau.utils.TestUtils.assertField;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static io.github.alien.roseau.utils.TestUtils.assertNoConstructor;
import static io.github.alien.roseau.utils.TestUtils.assertRecord;
import static io.github.alien.roseau.utils.TestUtils.buildSourcesAPI;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unresolved types spelled with a dotted name <em>and</em> type arguments, e.g.
 * {@code com.example.absent.Missing<String>} with no classpath. ecj does not recover them: the declarations that use
 * them are dropped, and supertypes and bounds become {@code java.lang.Object}. The same type is extracted correctly
 * when it is imported ({@code Missing<String>}) or used without type arguments ({@code com.example.absent.Missing}),
 * see {@link JdtTypesExtractorTest}. Roseau cannot rebuild these declarations, but records the missing types.
 */
class QualifiedParameterizedUnresolvedTypesTest {
	static final String MISSING = "com.example.absent.Missing";

	@TempDir
	Path wd;

	static void assertUnresolved(API api, String declaringType, String... types) {
		assertThat(api.getLibraryTypes().getUnresolvedTypes()).containsExactly(Map.entry(declaringType, Set.of(types)));
	}

	static void assertNoUnresolved(API api) {
		assertThat(api.getLibraryTypes().getUnresolvedTypes()).isEmpty();
	}

	@Test
	void field_type() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<String> f;
			}""");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void field_array_type() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<?>[] f;
			}""");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void field_type_argument() {
		var api = buildSourcesAPI("""
			public class A {
				public java.util.List<com.example.absent.Missing<String>> f;
			}""");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void only_the_lost_type_argument_of_a_resolved_type_is_recorded() {
		var api = buildSourcesAPI("""
			public class A {
				public java.util.Map.Entry<String, com.example.absent.M<String>> f;
			}""");

		assertUnresolved(api, "A", "com.example.absent.M");
	}

	@Test
	void every_lost_type_is_recorded() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<com.example.absent.Other<String>> f;
				public Unknown<com.example.absent.Missing<String>> g;
			}""");

		assertUnresolved(api, "A", MISSING, "com.example.absent.Other", "Unknown");
	}

	@Test
	void parameterized_qualifier() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Outer<String>.Inner f;
			}""");

		assertUnresolved(api, "A", "com.example.absent.Outer.Inner");
	}

	@Test
	void method_return_type() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<String> m() { return null; }
			}""");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void method_parameter_type() {
		var api = buildSourcesAPI("""
			public class A {
				public void m(int i, com.example.absent.Missing<String>... p) {}
			}""");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void method_thrown_type() {
		var api = buildSourcesAPI("""
			public class A {
				public void m() throws com.example.absent.Missing<String> {}
			}""");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void method_type_parameter_bound() {
		var api = buildSourcesAPI("""
			public class A {
				public <T extends com.example.absent.Missing<String>> void m(T t) {}
			}""");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void constructor_parameter_type() {
		var api = buildSourcesAPI("""
			public class A {
				public A(com.example.absent.Missing<String> p) {}
			}""");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void superclass() {
		var api = buildSourcesAPI("public class A extends com.example.absent.Missing<String> {}");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void implemented_interface() {
		var api = buildSourcesAPI("public class A implements com.example.absent.Missing<String> {}");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void extended_interface() {
		var api = buildSourcesAPI("public interface A extends com.example.absent.Missing<String> {}");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void type_parameter_bound() {
		var api = buildSourcesAPI("public class A<T extends com.example.absent.Missing<String>> {}");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void interface_members() {
		var api = buildSourcesAPI("""
			public interface A {
				com.example.absent.Missing<String> F = null;
				com.example.absent.Missing<String> m();
				default void d(com.example.absent.M<String> p) {}
				private void p(com.example.absent.Private<String> p) {}
			}""");

		assertUnresolved(api, "A", MISSING, "com.example.absent.M");
	}

	@Test
	void enum_abstract_method() {
		var api = buildSourcesAPI("""
			public enum A {
				X { public void m(com.example.absent.Missing<String> p) {} };
				public abstract void m(com.example.absent.Missing<String> p);
			}""");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void annotation_member() {
		var api = buildSourcesAPI("""
			public @interface A {
				Class<? extends com.example.absent.Missing<String>> value();
			}""");

		assertUnresolved(api, "A", MISSING);
	}

	@Test
	void nested_type() {
		var api = buildSourcesAPI("""
			public class A {
				public static class B {
					public com.example.absent.Missing<String> f;
				}
			}""");

		assertUnresolved(api, "A$B", MISSING);
	}

	@Test
	void record_component_and_made_up_canonical_constructor() {
		var api = buildSourcesAPI("public record A(com.example.absent.Missing<String> c, int i) {}");

		var a = assertRecord(api, "A");
		assertUnresolved(api, "A", MISSING);
		// ecj makes up A(int); it does not exist
		assertNoConstructor(api, a, "<init>(int)");
		assertMethod(api, a, "i()");
	}

	@Test
	void record_compact_canonical_constructor() {
		var api = buildSourcesAPI("""
			public record A(com.example.absent.Missing<String> c, int i) {
				public A {}
			}""");

		var a = assertRecord(api, "A");
		assertUnresolved(api, "A", MISSING);
		assertNoConstructor(api, a, "<init>(int)");
	}

	@Test
	void record_explicit_canonical_constructor() {
		var api = buildSourcesAPI("""
			public record A(com.example.absent.Missing<String> c, int i) {
				public A(com.example.absent.Missing<String> c, int i) { this.c = c; this.i = i; }
			}""");

		var a = assertRecord(api, "A");
		assertUnresolved(api, "A", MISSING);
		assertNoConstructor(api, a, "<init>(int)");
	}

	@Test
	void record_with_recovered_components_keeps_its_canonical_constructor() {
		var api = buildSourcesAPI("public record A(com.example.absent.Missing c, int i) {}");

		var a = assertRecord(api, "A");
		assertNoUnresolved(api);
		assertConstructor(api, a, "<init>(com.example.absent.Missing,int)");
	}

	@Test
	void members_that_are_not_extracted_are_ignored() {
		var api = buildSourcesAPI("""
			public class A {
				private com.example.absent.Missing<String> f;
				com.example.absent.Missing<String> g;
				A(com.example.absent.Missing<String> p) {}
				private void m(com.example.absent.Missing<String> p) {}
			}""");

		assertNoUnresolved(api);
	}

	@Test
	void recovered_spellings_are_not_recorded() {
		var api = buildSourcesAPI("""
			public class A<T extends java.util.List<String>> {
				public com.example.absent.Missing f;
				public Missing<String> g;
				public java.util.List<Missing<String>> h;
				public java.util.Map.Entry<String, java.util.List<? extends Number>>[] i;
			}""");

		var a = assertClass(api, "A");
		assertNoUnresolved(api);
		assertField(api, a, "f");
		assertField(api, a, "g");
		assertField(api, a, "h");
		assertField(api, a, "i");
	}

	@Test
	void imported_simple_names_are_recovered() throws IOException {
		Files.writeString(Files.createDirectories(wd.resolve("pkg")).resolve("A.java"), """
			package pkg;
			import com.example.absent.Missing;
			public class A<T extends Missing<String>> implements Missing<String> {
				public Missing<String> f;
				public java.util.List<Missing<String>> g;
				public void m(Missing<String> p) {}
			}""");

		var api = Roseau.buildAPI(Library.of(wd));

		var a = assertClass(api, "pkg.A");
		assertNoUnresolved(api);
		assertField(api, a, "f");
		assertField(api, a, "g");
		assertMethod(api, a, "m(com.example.absent.Missing)");
	}

	@Test
	void imported_qualifier_names_a_nested_type() throws IOException {
		Files.writeString(Files.createDirectories(wd.resolve("pkg")).resolve("A.java"), """
			package pkg;
			import com.example.absent.Outer;
			public class A {
				public Outer.Inner<String> f;
				public Outer<String>.Inner g;
			}""");

		var api = Roseau.buildAPI(Library.of(wd));

		assertUnresolved(api, "pkg.A", "com.example.absent.Outer$Inner");
	}
}
