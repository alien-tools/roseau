package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

// §13.4.19
class MethodNoLongerStaticTest {
	@Client("A.m();")
	@Test
	void method_no_longer_static() {
		var v1 = """
			public class A {
				public static void m() {}
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}""";

		assertBC("A", "A.m", BreakingChangeKind.METHOD_NO_LONGER_STATIC, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void private_method_no_longer_static() {
		var v1 = """
			public class A {
				private static void m() {}
			}""";
		var v2 = """
			public class A {
				private void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
