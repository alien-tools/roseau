package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertBC;
import static com.github.maracas.roseau.TestUtils.buildDiff;

class ConstructorRemovedTest {
	@Test
	void class_now_abstract() {
		String v1 = "public class A {}";
		String v2 = """
			public class A {
				public A(int i) {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1, buildDiff(v1, v2));
	}
}
