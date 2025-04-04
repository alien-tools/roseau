package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class FieldNowStaticTest {
	@Test
	void field_now_static() {
		var v1 = """
			public class A {
				public int f;
			}""";
		var v2 = """
			public class A {
				public static int f = 0;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_NOW_STATIC, 2, buildDiff(v1, v2));
	}
}
