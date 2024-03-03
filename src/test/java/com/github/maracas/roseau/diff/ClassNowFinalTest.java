package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

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
	void class_now_record() {
		String v1 = "public class A {}";
		String v2 = "public record A() {}";

		assertBC("A", BreakingChangeKind.CLASS_NOW_FINAL, 1, buildDiff(v1, v2));
	}

	@Test
	void record_now_final() {
		String v1 = "record A() {}";
		String v2 = "final record A() {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_now_effectively_final() {
		String v1 = "public class A {}";
		String v2 = """
			public class A {
				private A() {}
			}""";

		assertBC("A", BreakingChangeKind.CLASS_NOW_FINAL, 1, buildDiff(v1, v2));
	}

	@Test
	void nested_class_now_final() {
		String v1 ="""
			public class A {
				static public class B {}
			}""";
		String v2 ="""
			public class A {
				static public final class B {}
			}""";

		assertBC("A$B", BreakingChangeKind.CLASS_NOW_FINAL, 2, buildDiff(v1, v2));
	}
}
