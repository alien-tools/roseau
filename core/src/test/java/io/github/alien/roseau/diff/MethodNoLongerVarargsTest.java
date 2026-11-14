package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
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

		assertBC("A", "A.m(int[])", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("A", "A.m(java.lang.Object,int[])", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("A", "A.<init>(int[])", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("A", "A.<init>(java.lang.Object,int[])", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
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

	// Same erasure: the method still links and accepts arrays, but not variable arity invocations
	@Client("new A().m(1, 2);")
	@Test
	void method_varargs_to_array() {
		var v1 = """
			public class A {
				public void m(int... i) {}
			}""";
		var v2 = """
			public class A {
				public void m(int[] i) {}
			}""";

		assertBC("A", "A.m(int[])", BreakingChangeKind.EXECUTABLE_NO_LONGER_VARARGS, 2, buildDiff(v1, v2));
	}

	@Client("new A(null, 1, 2);")
	@Test
	void constructor_varargs_to_array() {
		var v1 = """
			public class A {
				public A(Object o, int... i) {}
			}""";
		var v2 = """
			public class A {
				public A(Object o, int[] i) {}
			}""";

		assertBC("A", "A.<init>(java.lang.Object,int[])", BreakingChangeKind.EXECUTABLE_NO_LONGER_VARARGS, 2,
			buildDiff(v1, v2));
	}

	@Client("new B().m(1, 2);")
	@Test
	void inherited_method_varargs_to_array() {
		var v1 = """
			public class A { public void m(int... i) {} }
			public class B extends A {}""";
		var v2 = """
			public class A { public void m(int[] i) {} }
			public class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m(int[])", BreakingChangeKind.EXECUTABLE_NO_LONGER_VARARGS, 1),
			bc("B", "A.m(int[])", BreakingChangeKind.EXECUTABLE_NO_LONGER_VARARGS, 1));
	}

	@Client("""
		new A().m(new int[] { 1, 2 });
		new A() { @Override public void m(int[] i) {} };""")
	@Test
	void method_array_to_varargs() {
		var v1 = """
			public class A {
				public void m(int[] i) {}
			}""";
		var v2 = """
			public class A {
				public void m(int... i) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
