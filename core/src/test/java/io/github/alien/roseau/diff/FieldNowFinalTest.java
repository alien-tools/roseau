package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class FieldNowFinalTest {
	@Client("new A().f = 0;")
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
