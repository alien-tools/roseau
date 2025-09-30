package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodNowFinalTest {
	@Client("new A() { @Override public void m() {} };")
	@Test
	void method_now_final() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public final void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NOW_FINAL, 2, buildDiff(v1, v2));
	}

	@Client("new A();")
	@Test
	void method_now_final_in_final_class() {
		var v1 = """
			public final class A {
				public void m() {}
			}""";
		var v2 = """
			public final class A {
				public final void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("// No uses")
	@Test
	void method_now_final_in_effectively_final_class() {
		var v1 = """
			public class A {
				private A() {}
				public void m() {}
			}""";
		var v2 = """
			public class A {
				private A() {}
				public final void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new B() { @Override public void m() {} };")
	@Test
	void method_now_final_in_subclass() {
		var v1 = """
			public class A {
				void m() {}
			}
			public class B extends A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				void m() {}
			}
			public class B extends A {
				public final void m() {}
			}""";

		assertBC("B", "B.m()", BreakingChangeKind.METHOD_NOW_FINAL, 2, buildDiff(v1, v2));
	}

	@Client("new B().m();")
	@Test
	void method_now_in_final_context_class() {
		var v1 = """
			public class A {
				public void m() {}
			}
			public final class B extends A {}""";
		var v2 = """
			public class A {
				public void m() {}
			}
			public final class B extends A {
				public void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new B().m();")
	@Test
	void method_now_in_final_context_interface() {
		var v1 = """
			public interface I {
				default void m() {}
			}
			public final class B implements I {}""";
		var v2 = """
			public interface I {
				default void m() {}
			}
			public final class B implements I {
				@Override public void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
