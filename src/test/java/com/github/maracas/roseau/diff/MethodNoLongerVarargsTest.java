package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class MethodNoLongerVarargsTest {
	@Test
	void method_no_longer_varargs_first() {
		var v1 = """
			public class A {
				public void m(int... i) {}
			}""";
		var v2 = """
			public class A {
				public void m(int i) {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void method_no_longer_varargs_last() {
		var v1 = """
			public class A {
				public void m(String s, int... i) {}
			}""";
		var v2 = """
			public class A {
				public void m(String s, int i) {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void method_overloaded_varargs() {
		var v1 = """
			public class A {
				public void m(String s) {}
				public void m(String... s) {}
			}""";
		var v2 = """
			public class A {
				public void m(String s) {}
				public void m(String... s) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void constructor_no_longer_varargs_first() {
		var v1 = """
			public class A {
				public A(int... i) {}
			}""";
		var v2 = """
			public class A {
				public A(int i) {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void constructor_no_longer_varargs_last() {
		var v1 = """
			public class A {
				public A(String s, int... i) {}
			}""";
		var v2 = """
			public class A {
				public A(String s, int i) {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void constructor_overloaded_varargs() {
		var v1 = """
			public class A {
				public A(String s) {}
				public A(String... s) {}
			}""";
		var v2 = """
			public class A {
				public A(String s) {}
				public A(String... s) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
