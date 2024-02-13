package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertBC;
import static com.github.maracas.roseau.TestUtils.buildDiff;

class ClassNowAbstractTest {
	@Test
	void class_now_abstract() {
		String v1 = "public class A {}";
		String v2 = "public abstract class A {}";

		assertBC("A", BreakingChangeKind.CLASS_NOW_ABSTRACT, 1, buildDiff(v1, v2));
	}
}
