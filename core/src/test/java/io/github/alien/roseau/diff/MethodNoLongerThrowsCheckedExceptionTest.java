package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodNoLongerThrowsCheckedExceptionTest {
	@Client("""
		try {
			new A().m();
		} catch (java.io.IOException e) {}""")
	@Test
	void method_no_longer_throws() {
		var v1 = """
			public class A {
				public void m() throws java.io.IOException {}
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Client("""
		try {
			new A();
		} catch (java.io.IOException e) {}""")
	@Test
	void constructor_no_longer_throws() {
		var v1 = """
			public class A {
				public A() throws java.io.IOException {}
			}""";
		var v2 = """
			public class A {
				public A() {}
			}""";

		assertBC("A", "A.<init>()", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Client("""
		try {
			new A().m();
		} catch (java.io.IOException e) {}""")
	@Test
	void final_method_no_longer_throws() {
		var v1 = """
			public class A {
				public final void m() throws java.io.IOException {}
			}""";
		var v2 = """
			public class A {
				public final void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Client("""
		try {
			new B().m();
		} catch (java.io.IOException e) {}""")
	@Test
	void method_no_longer_throws_indirect() {
		var v1 = """
			public class A {
				public void m() throws java.io.IOException {}
			}
			public class B extends A {}""";
		var v2 = """
			public class A {
				public void m() {}
			}
			public class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2),
			bc("B", "A.m()", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2));
	}

	@Client("""
		try {
			new A().m();
		} catch (java.io.IOException e) {}""")
	@Test
	void method_no_longer_throws_indirect_with_override_without_throws() {
		var v1 = """
			public class A {
				public void m() throws java.io.IOException {}
			}
			public class B extends A {
				@Override public void m() {}
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}
			public class B extends A {
				@Override public void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Client("""
		try {
			new A().m();
		} catch (RuntimeException e) {}""")
	@Test
	void method_no_longer_throws_unchecked() {
		var v1 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		try {
			new A<java.io.ObjectStreamException>().m();
		} catch (java.io.ObjectStreamException e) {}""")
	@Test
	void method_no_longer_throws_type_parameter() {
		var v1 = """
			public class A<T extends java.io.IOException> {
				public void m() throws T {}
			}""";
		var v2 = """
			public class A<T extends java.io.IOException> {
				public void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Client("""
		try {
			new A().<java.io.ObjectStreamException>m();
		} catch (java.io.ObjectStreamException e) {}""")
	@Test
	void method_no_longer_throws_type_parameter_method() {
		var v1 = """
			public class A {
				public <T extends java.io.IOException> void m() throws T {}
			}""";
		var v2 = """
			public class A {
				public <T extends java.io.IOException> void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}
}
