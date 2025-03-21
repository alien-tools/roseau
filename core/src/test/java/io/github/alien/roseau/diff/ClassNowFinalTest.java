package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

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
		var v1 = "public record A() {}";
		var v2 = "public final record A() {}";

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
		// An enum class is either implicitly final or implicitly sealed, as follows:
		//    An enum class is implicitly final if its declaration contains no enum constants
		//    that have a class body (§8.9.1).

		//    An enum class E is implicitly sealed if its declaration contains at least one enum
		//    constant that has a class body. The permitted direct subclasses (§8.1.6) of E are
		//    the anonymous classes implicitly declared by the enum constants that have a
		//    class body.
		var v1 = """
			public enum E {
				A;
			}""";
		var v2 = """
			public enum E {
				A, S { };
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
