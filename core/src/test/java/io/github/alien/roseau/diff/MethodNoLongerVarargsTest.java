package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodNoLongerVarargsTest {
	@Client("new A().m(1, 2);")
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

		assertBC("A", "A.m", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(null, 1, 2);")
	@Test
	void method_no_longer_varargs_last() {
		var v1 = """
			public class A {
				public void m(Object o, int... i) {}
			}""";
		var v2 = """
			public class A {
				public void m(Object o, int i) {}
			}""";

		assertBC("A", "A.m", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A().m(0);
		new A().m(1, 2);""")
	@Test
	void method_overloaded_varargs() {
		var v1 = """
			public class A {
				public void m(int i) {}
				public void m(int... i) {}
			}""";
		var v2 = """
			public class A {
				public void m(int i) {}
				public void m(int... i) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A a = new A(1, 2);")
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

		assertBC("A", "A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A(null, 1, 2);")
	@Test
	void constructor_no_longer_varargs_last() {
		var v1 = """
			public class A {
				public A(Object o, int... i) {}
			}""";
		var v2 = """
			public class A {
				public A(Object o, int i) {}
			}""";

		assertBC("A", "A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("""
		A a1 = new A(0);
		A a2 = new A(1, 2);""")
	@Test
	void constructor_overloaded_varargs() {
		var v1 = """
			public class A {
				public A(int i) {}
				public A(int... i) {}
			}""";
		var v2 = """
			public class A {
				public A(int i) {}
				public A(int... i) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
