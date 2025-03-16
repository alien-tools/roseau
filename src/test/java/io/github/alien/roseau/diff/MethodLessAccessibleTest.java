package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodLessAccessibleTest {
	@Test
	void method_now_protected() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				protected void m() {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NOW_PROTECTED, 2, buildDiff(v1, v2));
	}
}
