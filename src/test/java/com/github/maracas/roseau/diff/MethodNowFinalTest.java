package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class MethodNowFinalTest {
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

		assertBC("A.m", BreakingChangeKind.METHOD_NOW_FINAL, 2, buildDiff(v1, v2));
	}

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

		assertNoBC(BreakingChangeKind.METHOD_NOW_FINAL, buildDiff(v1, v2));
	}

	@Test
	void method_now_final_in_effectively_final_class() {
		var v1 = """
			public final class A {
				private A() {}
				public void m() {}
			}""";
		var v2 = """
			public final class A {
				private A() {}
				public final void m() {}
			}""";

		assertNoBC(BreakingChangeKind.METHOD_NOW_FINAL, buildDiff(v1, v2));
	}

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

		assertBC("B.m", BreakingChangeKind.METHOD_NOW_FINAL, 5, buildDiff(v1, v2));
	}
}
