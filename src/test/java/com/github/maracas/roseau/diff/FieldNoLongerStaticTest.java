package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class FieldNoLongerStaticTest {
	@Test
	void field_no_longer_static() {
		var v1 = """
			public class A {
				public static int f;
			}""";
		var v2 = """
			public class A {
				public int f = 0;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_NO_LONGER_STATIC, 2, buildDiff(v1, v2));
	}
}
