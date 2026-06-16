package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class FormalTypeParameterRemovedTest {
	@Client("A<String> a;")
	@Test
	void class_first_param_removed() {
		var v1 = "public class A<T> {}";
		var v2 = "public class A {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_REMOVED, 1, buildDiff(v1, v2));
	}

	@Client("A<String, String> a;")
	@Test
	void class_second_param_removed() {
		var v1 = "public class A<T, U> {}";
		var v2 = "public class A<T> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_REMOVED, 1, buildDiff(v1, v2));
	}

	@Client("new A().<String>m();")
	@Test
	void method_first_param_removed() {
		var v1 = """
			public class A {
				public <T> void m() {}
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.FORMAL_TYPE_PARAMETER_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new A().<String, Integer>m();")
	@Test
	void method_second_param_removed() {
		var v1 = """
			public class A {
				public <T, U> void m() {}
			}""";
		var v2 = """
			public class A {
				public <T> void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.FORMAL_TYPE_PARAMETER_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new A().<String>m();")
	@Test
	void final_method_all_params_removed_one() {
		var v1 = """
			public class A {
				public final <T> void m() {}
			}""";
		var v2 = """
			public class A {
				public final void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().<String, Integer>m();")
	@Test
	void final_method_all_params_removed_multiple() {
		var v1 = """
			public class A {
				public final <T, U> void m() {}
			}""";
		var v2 = """
			public class A {
				public final void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().<String, Integer>m();")
	@Test
	void final_method_param_removed_but_still_generic() {
		var v1 = """
			public class A {
				public final <T, U> void m() {}
			}""";
		var v2 = """
			public class A {
				public final <T> void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.FORMAL_TYPE_PARAMETER_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new <String, Integer>A();")
	@Test
	void constructor_all_params_removed_multiple() {
		var v1 = """
			public class A {
				public <T, U> A() {}
			}""";
		var v2 = """
			public class A {
				public A() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new <String, Integer>A();")
	@Test
	void constructor_param_removed_but_still_generic() {
		var v1 = """
			public class A {
				public <T, U> A() {}
			}""";
		var v2 = """
			public class A {
				public <T> A() {}
			}""";

		assertBC("A", "A.<init>()", BreakingChangeKind.FORMAL_TYPE_PARAMETER_REMOVED, 2, buildDiff(v1, v2));
	}
}
