package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class ClassNowFinalTest {
	@Test
	void class_now_final() {
		var v1 = "public class A {}";
		var v2 = "public final class A {}";

		assertBC("A", BreakingChangeKind.CLASS_NOW_FINAL, 1, buildDiff(v1, v2));
	}

	@Test
	void class_now_sealed() {
		var v1 = "public class A {}";
		var v2 = """
			public sealed class A permits B {}
			final class B extends A {}""";

		assertBC("A", BreakingChangeKind.CLASS_NOW_FINAL, 1, buildDiff(v1, v2));
	}

	@Test
	void class_now_record() {
		var v1 = "public class A {}";
		var v2 = "public record A() {}";

		assertBC("A", BreakingChangeKind.CLASS_NOW_FINAL, 1, buildDiff(v1, v2));
	}

	@Test
	void record_now_final() {
		var v1 = "record A() {}";
		var v2 = "final record A() {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_now_effectively_final() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				private A() {}
			}""";

		assertBC("A", BreakingChangeKind.CLASS_NOW_FINAL, 1, buildDiff(v1, v2));
	}

	@Test
	void nested_class_now_final() {
		var v1 = """
			public class A {
				static public class B {}
			}""";
		var v2 = """
			public class A {
				static public final class B {}
			}""";

		assertBC("A$B", BreakingChangeKind.CLASS_NOW_FINAL, 2, buildDiff(v1, v2));
	}

	@Test
	void enummm() {
		// JLS §8.9:
		// An enum class E is implicitly sealed if its declaration contains at least one enum
		// constant that has a class body. The permitted direct subclasses (§8.1.6) of E are
		// the anonymous classes implicitly declared by the enum constants that have a class body
		var v1 = """
			public enum E {
				A;
			}""";
		var v2 = """
			public enum E {
				A, S { };
			}""";

		assertBC("E", BreakingChangeKind.CLASS_NOW_FINAL, 1, buildDiff(v1, v2));
	}
}
