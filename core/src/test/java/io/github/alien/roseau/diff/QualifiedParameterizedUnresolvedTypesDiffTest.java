package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

/**
 * Breaking changes involving an unresolved type spelled with its qualified name and type arguments, e.g.
 * {@code unknown.Unknown<String>} with no classpath. The expected breaking changes are those Roseau reports when the
 * same type is imported instead ({@code import unknown.Unknown;} and {@code Unknown<String>}). With the qualified
 * spelling, the members and supertypes are missing from the API model, so these changes go unreported, or are
 * reported as the wrong kind.
 */
class QualifiedParameterizedUnresolvedTypesDiffTest {
	@Test
	void method_removed() {
		var v1 = """
			public class A {
				public void m(unknown.Unknown<String> p) {}
			}""";
		var v2 = "public class A {}";

		assertBC("A", "A.m(unknown.Unknown<java.lang.String>)", BreakingChangeKind.EXECUTABLE_REMOVED, 2,
			buildDiff(v1, v2));
	}

	@Test
	void field_removed() {
		var v1 = """
			public class A {
				public unknown.Unknown<String> f;
			}""";
		var v2 = "public class A {}";

		assertBC("A", "A.f", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void constructor_removed() {
		var v1 = """
			public class A {
				public A() {}
				public A(unknown.Unknown<String> p) {}
			}""";
		var v2 = """
			public class A {
				public A() {}
			}""";

		assertBC("A", "A.<init>(unknown.Unknown<java.lang.String>)", BreakingChangeKind.EXECUTABLE_REMOVED, 3,
			buildDiff(v1, v2));
	}

	@Test
	void record_component_removed() {
		var v1 = "public record A(unknown.Unknown<String> c, int i) {}";
		var v2 = "public record A(int i) {}";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.<init>(unknown.Unknown<java.lang.String>,int)", BreakingChangeKind.EXECUTABLE_REMOVED, -1),
			bc("A", "A.c()", BreakingChangeKind.EXECUTABLE_REMOVED, -1));
	}

	@Test
	void parameter_type_arguments_changed() {
		var v1 = """
			public class A {
				public void m(unknown.Unknown<String> p) {}
			}""";
		var v2 = """
			public class A {
				public void m(unknown.Unknown<Integer> p) {}
			}""";

		assertBC("A", "A.m(unknown.Unknown<java.lang.String>)", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED,
			2, buildDiff(v1, v2));
	}

	@Test
	void return_type_changed_is_not_a_removal() {
		var v1 = """
			public class A {
				public String m() { return null; }
			}""";
		var v2 = """
			public class A {
				public unknown.Unknown<String> m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Test
	void superclass_removed() {
		var v1 = "public class A extends unknown.Unknown<String> {}";
		var v2 = "public class A {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_SUPERTYPE_REMOVED, 1, buildDiff(v1, v2));
	}

	@Test
	void implemented_interface_removed() {
		var v1 = "public class A implements unknown.Unknown<String> {}";
		var v2 = "public class A {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_SUPERTYPE_REMOVED, 1, buildDiff(v1, v2));
	}

	@Test
	void type_parameter_bound_changed() {
		var v1 = "public class A<T extends unknown.Unknown<String>> {}";
		var v2 = "public class A<T extends unknown.Unknown<Integer>> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}
}
