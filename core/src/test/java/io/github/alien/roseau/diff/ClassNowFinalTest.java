package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class ClassNowFinalTest {
	@Client("new A(){};")
	@Test
	void class_now_final() {
		var v1 = "public class A {}";
		var v2 = "public final class A {}";

		assertBC("A", "A", BreakingChangeKind.CLASS_NOW_FINAL, 1, buildDiff(v1, v2));
	}

	@Client("new B(){};")
	@Test
	void subclass_now_final() {
		var v1 = """
			public class A {
				public void m() {}
			}
			public class B extends A {}""";
		var v2 = """
			public class A {
				public void m() {}
			}
			public final class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("B", "B", BreakingChangeKind.CLASS_NOW_FINAL, 1),
			bc("B", "A.m()", BreakingChangeKind.METHOD_NOW_FINAL, 2));
	}

	@Client("new A(){};")
	@Test
	void class_now_sealed() {
		var v1 = "public class A {}";
		var v2 = """
			public sealed class A permits B {}
			final class B extends A {}""";

		assertBC("A", "A", BreakingChangeKind.CLASS_NOW_FINAL, 1, buildDiff(v1, v2));
	}

	@Client("new A(){};")
	@Test
	void class_now_record() {
		var v1 = "public class A {}";
		var v2 = "public record A() {}";

		assertBC("A", "A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("new A();")
	@Test
	void record_now_final() {
		var v1 = "public record A() {}";
		var v2 = "public final record A() {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A(){};")
	@Test
	void class_now_effectively_final() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				private A() {}
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A", BreakingChangeKind.CLASS_NOW_FINAL, 1),
			bc("A", "A", BreakingChangeKind.CLASS_NOW_ABSTRACT, 1),
			bc("A", "A.<init>()", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1));
	}

	@Client("new A.B(){};")
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

		assertBC("A$B", "A$B", BreakingChangeKind.CLASS_NOW_FINAL, 2, buildDiff(v1, v2));
	}

	@Client("E a = E.A;")
	@Test
	void enummm() {
		// JLS §8.9:
		// An enum class is either implicitly final or implicitly sealed, as follows:
		// An enum class is implicitly final if its declaration contains no enum constants
		// that have a class body (§8.9.1).

		// An enum class E is implicitly sealed if its declaration contains at least one enum
		// constant that has a class body. The permitted direct subclasses (§8.1.6) of E are
		// the anonymous classes implicitly declared by the enum constants that have a
		// class body.
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
