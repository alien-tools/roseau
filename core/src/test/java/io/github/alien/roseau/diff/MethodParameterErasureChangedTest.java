package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;
import static org.assertj.core.api.Assertions.assertThat;

class MethodParameterErasureChangedTest {
	// Subclasses that extend the raw A override m(Number), which becomes m(Object) (§4.8)
	@Client("""
		new B().m(1);
		new A() { @Override public void m(Number n) {} };""")
	@Test
	void supertype_bound_widened() {
		var v1 = """
			public class A<T extends Number> { public void m(T t) {} }
			public class B extends A<Integer> {}""";
		var v2 = """
			public class A<T> { public void m(T t) {} }
			public class B extends A<Integer> {}""";

		// B.m(Number) becomes B.m(Object), while B.m(Integer) remains in source code
		var bcs = buildDiff(v1, v2);
		assertBCs(bcs,
			bc("A", "A.m(T)", BreakingChangeKind.METHOD_PARAMETER_ERASURE_CHANGED, 1),
			bc("A", "A.m(T)", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1),
			bc("B", "A.m(java.lang.Integer)", BreakingChangeKind.METHOD_PARAMETER_ERASURE_CHANGED, 1));
		assertThat(bcs)
			.filteredOn(bc -> bc.kind() == BreakingChangeKind.METHOD_PARAMETER_ERASURE_CHANGED)
			.extracting(bc -> bc.details())
			.containsOnly(new BreakingChangeDetails.MethodParameterErasureChanged(
				"m(java.lang.Number)", "m(java.lang.Object)"));
	}

	@Client("new B().m(1);")
	@Test
	void package_private_supertype_bound_widened_binary_only() {
		var v1 = """
			class A<T extends Number> { public void m(T t) {} }
			public class B extends A<Integer> {}""";
		var v2 = """
			class A<T> { public void m(T t) {} }
			public class B extends A<Integer> {}""";

		assertBC("B", "A.m(java.lang.Integer)", BreakingChangeKind.METHOD_PARAMETER_ERASURE_CHANGED, 1,
			buildDiff(v1, v2));
	}

	@Client("new B().m(\"\");")
	@Test
	void specializing_override_removed_binary_only() {
		var v1 = """
			public class A<T> { public void m(T t) {} }
			public class B extends A<String> { @Override public void m(String s) {} }""";
		var v2 = """
			public class A<T> { public void m(T t) {} }
			public class B extends A<String> {}""";

		// B.m(String) is gone, only A.m(Object) remains
		assertBC("B", "B.m(java.lang.String)", BreakingChangeKind.METHOD_PARAMETER_ERASURE_CHANGED, 1,
			buildDiff(v1, v2));
	}

	@Client("new B().m(\"\");")
	@Test
	void specializing_override_added_no_break() {
		var v1 = """
			public class A<T> { public void m(T t) {} }
			public class B extends A<String> {}""";
		var v2 = """
			public class A<T> { public void m(T t) {} }
			public class B extends A<String> { @Override public void m(String s) {} }""";

		// A.m(Object) remains linkable through B
		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new B().m(1);")
	@Test
	void supertype_bound_unchanged_no_break() {
		var v1 = """
			public class A<T extends Number> { public void m(T t) {} }
			public class B extends A<Integer> {}""";
		var v2 = """
			public class A<U extends Number> { public void m(U u) {} }
			public class B extends A<Integer> {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// A.m(Number) becomes A.m(Object): parameterized invocations and overriders still compile, but no longer link, and
	// subclasses that extend the raw A no longer override it (§4.8)
	@Client("""
		new A<Integer>().m(1);
		new A<Integer>() { @Override public void m(Integer i) {} };
		new A() { @Override public void m(Number n) {} };""")
	@Test
	void declaring_type_bound_widened() {
		var v1 = """
			public class A<T extends Number> { public void m(T t) {} }""";
		var v2 = """
			public class A<T> { public void m(T t) {} }""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m(T)", BreakingChangeKind.METHOD_PARAMETER_ERASURE_CHANGED, 1),
			bc("A", "A.m(T)", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1));
	}

	// The parameterized invocations of a final method still compile, and so do the raw ones: m(Object) accepts Numbers
	@Client("""
		new A<Integer>().m(1);
		new A().m(1);""")
	@Test
	void declaring_type_bound_widened_final_binary_only() {
		var v1 = """
			public class A<T extends Number> { public final void m(T t) {} }""";
		var v2 = """
			public class A<T> { public final void m(T t) {} }""";

		assertBC("A", "A.m(T)", BreakingChangeKind.METHOD_PARAMETER_ERASURE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("""
		I<Integer> i = n -> {};
		class C implements I { public void m(Number n) {} }""")
	@Test
	void declaring_interface_bound_widened() {
		var v1 = """
			public interface I<T extends Number> { void m(T t); }""";
		var v2 = """
			public interface I<T> { void m(T t); }""";

		assertBCs(buildDiff(v1, v2),
			bc("I", "I.m(T)", BreakingChangeKind.METHOD_PARAMETER_ERASURE_CHANGED, 1),
			bc("I", "I.m(T)", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1));
	}
}
