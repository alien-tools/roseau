package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class MethodNowStaticTest {
	@Test
	void method_now_static() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public static void m() {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NOW_STATIC, 2, buildDiff(v1, v2));
	}
}
