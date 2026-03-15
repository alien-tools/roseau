package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

// §13.4.19
class MethodNowStaticTest {
	@Client("""
		new A().m();
		new A() { @Override public void m() {} };""")
	@Test
	void overridable_method_now_static() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public static void m() {}
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_NOW_STATIC, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_OVERRIDABLE_NOW_STATIC, 2));
	}

	@Client("new A().m();")
	@Test
	void final_method_now_static() {
		var v1 = """
			public class A {
				public final void m() {}
			}""";
		var v2 = """
			public class A {
				public static void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NOW_STATIC, 2, buildDiff(v1, v2));
	}

	@Client("new A().m();")
	@Test
	void method_in_final_class_now_static() {
		var v1 = """
			public final class A {
				public void m() {}
			}""";
		var v2 = """
			public final class A {
				public static void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NOW_STATIC, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void private_method_now_static() {
		var v1 = """
			public class A {
				private void m() {}
			}""";
		var v2 = """
			public class A {
				private static void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("I i = null; i.m();")
	@Test
	void sealed_interface_method_now_static() {
		var v1 = """
			public sealed interface I permits A {
				default void m() {}
			}
			final class A implements I {}""";
		var v2 = """
			public sealed interface I permits A {
				static void m() {}
			}
			final class A implements I {}""";

		assertBCs(buildDiff(v1, v2),
			bc("I", "I.m()", BreakingChangeKind.METHOD_NOW_STATIC, 2),
			bc("I", "I.m()", BreakingChangeKind.METHOD_OVERRIDABLE_NOW_STATIC, 2));
	}
}
