package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertBC;
import static com.github.maracas.roseau.TestUtils.assertNoBC;
import static com.github.maracas.roseau.TestUtils.buildDiff;

class ClassNowFinalTest {
	@Test
	void class_now_final() {
		String v1 = "public class A {}";
		String v2 = "public final class A {}";

		assertBC("A", BreakingChangeKind.CLASS_NOW_FINAL, 1, buildDiff(v1, v2));
	}

	@Test
	void class_now_sealed() {
		String v1 = "public class A {}";
		String v2 = """
			public sealed class A permits B {}
			final class B extends A {}""";

		assertBC("A", BreakingChangeKind.CLASS_NOW_FINAL, 1, buildDiff(v1, v2));
	}

	@Test
	void record_now_final() {
		String v1 = "record A() {}";
		String v2 = "final record A() {}";

		assertNoBC(buildDiff(v1, v2));
	}
}
