package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodLessAccessibleTest {
	@Client("new A().m();")
	@Test
	void public_to_protected() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				protected void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NOW_PROTECTED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A() {
			@Override protected void m() {}
		};""")
	@Test
	void protected_to_package_private() {
		var v1 = """
			public class A {
				protected void m() {}
			}""";
		var v2 = """
			public class A {
				void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void package_private_to_private() {
		var v1 = """
			public class A {
				void m() {}
			}""";
		var v2 = """
			public class A {
				private void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
