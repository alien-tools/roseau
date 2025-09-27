package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodNowThrowsCheckedExceptionTest {
	@Client("new A().m();")
	@Test
	void method_now_throws() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public void m() throws Exception {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Client("new B().m();")
	@Test
	void method_now_throws_indirect() {
		var v1 = """
			public class A {
				public void m() {}
			}
			public class B extends A {}""";
		var v2 = """
			public class A {
				public void m() throws Exception {}
			}
			public class B extends A {}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Client("new B().m();")
	@Test
	void method_now_throws_indirect_with_override_without_throws() {
		var v1 = """
			public class A {
				public void m() {}
			}
			public class B extends A {
				@Override public void m() {}
			}""";
		var v2 = """
			public class A {
				public void m() throws Exception {}
			}
			public class B extends A {
				@Override public void m() {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Client("""
		try {
			new A().m();
		} catch (java.io.IOException e) {}""")
	@Test
	void method_now_throws_unchecked() {
		var v1 = """
			public class A {
				public void m() throws java.io.IOException {}
			}""";
		var v2 = """
			public class A {
				public void m() throws IllegalArgumentException {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
		assertNoBC(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, buildDiff(v1, v2));
	}

	@Client("""
		try {
			new A().m();
		} catch (java.io.IOException e) {}""")
	@Test
	void method_now_throws_subtype() {
		var v1 = """
			public class A {
				public void m() throws java.io.IOException {}
			}""";
		var v2 = """
			public class A {
				public void m() throws java.io.ObjectStreamException {}
			}""";

		assertNoBC(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, buildDiff(v1, v2));
	}

	@Client("""
		try {
			new A().m();
		} catch (java.io.ObjectStreamException e) {}""")
	@Test
	void method_now_throws_supertype() {
		var v1 = """
			public class A {
				public void m() throws java.io.ObjectStreamException {}
			}""";
		var v2 = """
			public class A {
				public void m() throws java.io.IOException {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}
}
