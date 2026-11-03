package io.github.alien.roseau.extractors.jdt;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.ExtractionGap;
import io.github.alien.roseau.api.model.SourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertConstructor;
import static io.github.alien.roseau.utils.TestUtils.assertField;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static io.github.alien.roseau.utils.TestUtils.assertNoConstructor;
import static io.github.alien.roseau.utils.TestUtils.assertRecord;
import static io.github.alien.roseau.utils.TestUtils.buildSourcesAPI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Unresolved types spelled with a dotted name <em>and</em> type arguments, e.g.
 * {@code com.example.absent.Missing<String>} with no classpath. ecj does not recover them: the declarations that use
 * them are dropped, and supertypes and bounds become {@code java.lang.Object}. The same type is extracted correctly
 * when it is imported ({@code Missing<String>}) or used without type arguments ({@code com.example.absent.Missing}),
 * see {@link JdtTypesExtractorTest}. Roseau cannot rebuild these declarations and records them as extraction gaps.
 */
class QualifiedParameterizedUnresolvedTypesTest {
	static final String MISSING = "com.example.absent.Missing";

	@TempDir
	Path wd;

	static ExtractionGap gap(String declaringType, String declaration, String reference, int line) {
		String file = declaringType.split("\\$")[0].replace('.', '/') + ".java";
		return new ExtractionGap(declaringType, declaration, reference, new SourceLocation(Path.of(file), line),
			ExtractionGap.Reason.UNRESOLVED_TYPE);
	}

	static ExtractionGap gap(String declaringType, String declaration, int line) {
		return gap(declaringType, declaration, MISSING, line);
	}

	@Test
	void field_type() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<String> f;
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.f", 2));
	}

	@Test
	void each_field_fragment() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<String> f, g[];
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.f", 2), gap("A", "A.g", 2));
	}

	@Test
	void field_array_type() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<String>[] f;
				public com.example.absent.Missing<?>[] g;
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.f", 2), gap("A", "A.g", 3));
	}

	@Test
	void field_type_argument() {
		var api = buildSourcesAPI("""
			public class A {
				public java.util.List<com.example.absent.Missing<String>> f;
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.f", 2));
	}

	@Test
	void only_the_lost_type_argument_of_a_resolved_type_is_reported() {
		var api = buildSourcesAPI("""
			public class A {
				public java.util.Map.Entry<String, com.example.absent.M<String>> f;
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.f", "com.example.absent.M", 2));
	}

	@Test
	void every_lost_type_is_reported() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<com.example.absent.Other<String>> f;
			}""");

		assertThat(api.getExtractionGaps()).containsExactlyInAnyOrder(
			gap("A", "A.f", 2),
			gap("A", "A.f", "com.example.absent.Other", 2));
	}

	@Test
	void parameterized_qualifier() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Outer<String>.Inner f;
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.f", "com.example.absent.Outer.Inner", 2));
	}

	@Test
	void method_return_type() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<String> m() { return null; }
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.m()", 2));
	}

	@Test
	void method_parameter_type() {
		var api = buildSourcesAPI("""
			public class A {
				public void m(int i, com.example.absent.Missing<String> p) {}
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(
			gap("A", "A.m(int, com.example.absent.Missing<String>)", 2));
	}

	@Test
	void method_varargs_parameter_type() {
		var api = buildSourcesAPI("""
			public class A {
				public void m(com.example.absent.Missing<String>... p) {}
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.m(com.example.absent.Missing<String>...)", 2));
	}

	@Test
	void method_thrown_type() {
		var api = buildSourcesAPI("""
			public class A {
				public void m() throws com.example.absent.Missing<String> {}
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.m()", 2));
	}

	@Test
	void constructor_parameter_type() {
		var api = buildSourcesAPI("""
			public class A {
				public A(com.example.absent.Missing<String> p) {}
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.<init>(com.example.absent.Missing<String>)", 2));
	}

	@Test
	void record_component_and_made_up_canonical_constructor() {
		var api = buildSourcesAPI("public record A(com.example.absent.Missing<String> c, int i) {}");

		var a = assertRecord(api, "A");
		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.c", 1));
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
		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.c", 1));
		assertNoConstructor(api, a, "<init>(int)");
	}

	@Test
	void record_explicit_canonical_constructor_and_accessor() {
		var api = buildSourcesAPI("""
			public record A(com.example.absent.Missing<String> c, int i) {
				public A(com.example.absent.Missing<String> c, int i) { this.c = c; this.i = i; }
				public com.example.absent.Missing<String> c() { return c; }
			}""");

		var a = assertRecord(api, "A");
		assertThat(api.getExtractionGaps()).containsExactly(
			gap("A", "A.c", 1),
			gap("A", "A.<init>(com.example.absent.Missing<String>, int)", 2),
			gap("A", "A.c()", 3));
		assertNoConstructor(api, a, "<init>(int)");
	}

	@Test
	void record_with_resolved_components_keeps_its_canonical_constructor() {
		var api = buildSourcesAPI("public record A(com.example.absent.Missing c, int i) {}");

		var a = assertRecord(api, "A");
		assertThat(api.getExtractionGaps()).isEmpty();
		assertConstructor(api, a, "<init>(com.example.absent.Missing,int)");
	}

	@Test
	void superclass() {
		var api = buildSourcesAPI("public class A extends com.example.absent.Missing<String> {}");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A", 1));
	}

	@Test
	void implemented_interface() {
		var api = buildSourcesAPI("public class A implements com.example.absent.Missing<String> {}");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A", 1));
	}

	@Test
	void extended_interface() {
		var api = buildSourcesAPI("public interface A extends com.example.absent.Missing<String> {}");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A", 1));
	}

	@Test
	void type_parameter_bound() {
		var api = buildSourcesAPI("public class A<T extends com.example.absent.Missing<String>> {}");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A", 1));
	}

	@Test
	void method_type_parameter_bound() {
		var api = buildSourcesAPI("""
			public class A {
				public <T extends com.example.absent.Missing<String>> void m(T t) {}
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.m(T)", 2));
	}

	@Test
	void interface_members() {
		var api = buildSourcesAPI("""
			public interface A {
				com.example.absent.Missing<String> F = null;
				com.example.absent.Missing<String> m();
				default void d(com.example.absent.Missing<String> p) {}
				private void p(com.example.absent.Missing<String> p) {}
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(
			gap("A", "A.F", 2),
			gap("A", "A.m()", 3),
			gap("A", "A.d(com.example.absent.Missing<String>)", 4));
	}

	@Test
	void enum_abstract_method() {
		var api = buildSourcesAPI("""
			public enum A {
				X { public void m(com.example.absent.Missing<String> p) {} };
				public abstract void m(com.example.absent.Missing<String> p);
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.m(com.example.absent.Missing<String>)", 3));
	}

	@Test
	void annotation_member() {
		var api = buildSourcesAPI("""
			public @interface A {
				Class<? extends com.example.absent.Missing<String>> value();
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A", "A.value()", 2));
	}

	@Test
	void nested_type() {
		var api = buildSourcesAPI("""
			public class A {
				public static class B {
					public com.example.absent.Missing<String> f;
				}
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(gap("A$B", "A$B.f", 3));
	}

	@Test
	void members_outside_the_extracted_api_are_ignored() {
		var api = buildSourcesAPI("""
			public class A {
				private com.example.absent.Missing<String> f;
				com.example.absent.Missing<String> g;
				A(com.example.absent.Missing<String> p) {}
				private void m(com.example.absent.Missing<String> p) {}
			}""");

		assertThat(api.getExtractionGaps()).isEmpty();
	}

	@Test
	void recovered_spellings_are_not_gaps() {
		var api = buildSourcesAPI("""
			public class A<T extends java.util.List<String>> {
				public com.example.absent.Missing f;
				public Missing<String> g;
				public java.util.List<Missing<String>> h;
				public java.util.Map.Entry<String, java.util.List<? extends Number>>[] i;
			}""");

		var a = assertClass(api, "A");
		assertThat(api.getExtractionGaps()).isEmpty();
		assertField(api, a, "f");
		assertField(api, a, "g");
		assertField(api, a, "h");
		assertField(api, a, "i");
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

		assertThat(api.getExtractionGaps()).containsExactly(
			gap("pkg.A", "pkg.A.f", "com.example.absent.Outer$Inner", 4),
			gap("pkg.A", "pkg.A.g", "com.example.absent.Outer$Inner", 5));
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
		assertThat(api.getExtractionGaps()).isEmpty();
		assertField(api, a, "f");
		assertField(api, a, "g");
		assertMethod(api, a, "m(com.example.absent.Missing)");
	}

	@Test
	void simple_name_with_a_lost_type_argument() {
		var api = buildSourcesAPI("""
			public class A {
				public java.util.List<com.example.absent.Missing<String>> f;
				public Unknown<com.example.absent.Missing<String>> g;
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(
			gap("A", "A.f", 2),
			gap("A", "A.g", "Unknown", 3),
			gap("A", "A.g", 3));
	}

	@Test
	void unknown_constant_value() {
		var api = buildSourcesAPI("""
			public class A {
				public static final int C = com.example.absent.K.VALUE;
				public static final String D = "a" + Unknown.VALUE;
				public static final int E = com.example.absent.K.compute();
				public static final int F = 1;
			}""");

		var a = assertClass(api, "A");
		assertThat(api.getExtractionGaps()).containsExactly(
			new ExtractionGap("A", "A.C", null, new SourceLocation(Path.of("A.java"), 2),
				ExtractionGap.Reason.UNKNOWN_CONSTANT),
			new ExtractionGap("A", "A.D", null, new SourceLocation(Path.of("A.java"), 3),
				ExtractionGap.Reason.UNKNOWN_CONSTANT));
		assertThat(assertField(api, a, "F").isCompileTimeConstant()).isTrue();
	}

	@Test
	void type_without_binding() {
		var api = buildSourcesAPI("""
			public class A {
				public static class B {}
				public static class B {}
			}""");

		assertThat(api.getExtractionGaps()).containsExactly(new ExtractionGap("A$B", "A$B", null,
			new SourceLocation(Path.of("A.java"), 3), ExtractionGap.Reason.NO_BINDING));
	}

	@Test
	void member_without_binding() {
		var api = buildSourcesAPI("""
			public class A {
				public void m() {}
				public void m() {}
			}""");

		assertThat(api.getExtractionGaps())
			.extracting(ExtractionGap::declaration, ExtractionGap::reason)
			.containsExactly(tuple("A.m()", ExtractionGap.Reason.NO_BINDING));
	}
}
