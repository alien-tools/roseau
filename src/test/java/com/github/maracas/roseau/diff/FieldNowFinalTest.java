package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class FieldNowFinalTest {
	@Test
	void field_now_final() {
		var v1 = """
			public class A {
				public int f;
			}""";
		var v2 = """
			public class A {
				public final int f = 0;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_NOW_FINAL, 2, buildDiff(v1, v2));
	}
}
