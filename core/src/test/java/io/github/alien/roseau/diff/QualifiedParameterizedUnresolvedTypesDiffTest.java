package io.github.alien.roseau.diff;

import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauException;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.buildSourcesAPI;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Changes involving an unresolved type spelled with a dotted name and type arguments, e.g.
 * {@code unknown.Unknown<String>} with no classpath. ecj does not recover such types, and the declarations using them
 * are missing from the API model or distorted, so the breaking changes are unreported or of the wrong kind. Roseau
 * cannot extract these declarations faithfully; instead, it reports the missing types as unresolved.
 * <p>
 * The expected breaking changes, were these declarations extracted, are those Roseau reports when the same type is
 * imported instead ({@code import unknown.Unknown;} and {@code Unknown<String>}).
 */
class QualifiedParameterizedUnresolvedTypesDiffTest {
	static void assertIncomplete(String v1, String v2) {
		var report = Roseau.diff(buildSourcesAPI(v1), buildSourcesAPI(v2));

		assertThatThrownBy(report::checkFullyResolved)
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("unknown.Unknown");
	}

	// Expected: A.m(unknown.Unknown<java.lang.String>) EXECUTABLE_REMOVED
	@Test
	void method_removed() {
		var v1 = """
			public class A {
				public void m(unknown.Unknown<String> p) {}
			}""";
		var v2 = "public class A {}";

		assertIncomplete(v1, v2);
	}

	// Expected: A.f FIELD_REMOVED
	@Test
	void field_removed() {
		var v1 = """
			public class A {
				public unknown.Unknown<String> f;
			}""";
		var v2 = "public class A {}";

		assertIncomplete(v1, v2);
	}

	// Expected: A.<init>(unknown.Unknown<java.lang.String>) EXECUTABLE_REMOVED
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

		assertIncomplete(v1, v2);
	}

	// Expected: A.<init>(unknown.Unknown<java.lang.String>,int) and A.c() EXECUTABLE_REMOVED
	@Test
	void record_component_removed() {
		var v1 = "public record A(unknown.Unknown<String> c, int i) {}";
		var v2 = "public record A(int i) {}";

		assertIncomplete(v1, v2);
	}

	// Expected: A.m(unknown.Unknown<java.lang.String>) EXECUTABLE_PARAMETER_GENERICS_CHANGED
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

		assertIncomplete(v1, v2);
	}

	// Expected: A.m() METHOD_RETURN_TYPE_ERASURE_CHANGED and METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, not a removal
	@Test
	void return_type_changed() {
		var v1 = """
			public class A {
				public String m() { return null; }
			}""";
		var v2 = """
			public class A {
				public unknown.Unknown<String> m() { return null; }
			}""";

		assertIncomplete(v1, v2);
	}

	// Expected: A TYPE_SUPERTYPE_REMOVED
	@Test
	void superclass_removed() {
		var v1 = "public class A extends unknown.Unknown<String> {}";
		var v2 = "public class A {}";

		assertIncomplete(v1, v2);
	}

	// Expected: A TYPE_SUPERTYPE_REMOVED
	@Test
	void implemented_interface_removed() {
		var v1 = "public class A implements unknown.Unknown<String> {}";
		var v2 = "public class A {}";

		assertIncomplete(v1, v2);
	}

	// Expected: A FORMAL_TYPE_PARAMETER_CHANGED
	@Test
	void type_parameter_bound_changed() {
		var v1 = "public class A<T extends unknown.Unknown<String>> {}";
		var v2 = "public class A<T extends unknown.Unknown<Integer>> {}";

		assertIncomplete(v1, v2);
	}
}
