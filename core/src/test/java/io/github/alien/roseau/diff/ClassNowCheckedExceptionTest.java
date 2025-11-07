package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class ClassNowCheckedExceptionTest {
	@Client("new A();")
	@Test
	void class_becomes_generic_checked_exception() {
		var v1 = "public class A {}";
		var v2 = "public class A extends Exception {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A();")
	@Test
	void class_becomes_specific_checked_exception() {
		var v1 = "public class A {}";
		var v2 = "public class A extends java.io.IOException {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("throw new A();")
	@Test
	void unchecked_exception_becomes_checked_exception() {
		var v1 = "public class A extends RuntimeException {}";
		var v2 = "public class A extends Exception {}";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A", BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, 1),
			bc("A", "A", BreakingChangeKind.SUPERTYPE_REMOVED, 1));
	}

	@Client("""
		try {
			throw new A();
		} catch (Throwable t) {}""")
	@Test
	void throwable_becomes_checked_exception() {
		var v1 = "public class A extends Throwable {}";
		var v2 = "public class A extends Exception {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("throw new A();")
	@Test
	void error_becomes_checked_exception() {
		var v1 = "public class A extends Error {}";
		var v2 = "public class A extends Exception {}";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A", BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, 1),
			bc("A", "A", BreakingChangeKind.SUPERTYPE_REMOVED, 1));
	}

	@Client("throw new A();")
	@Test
	void specific_unchecked_exception_becomes_specific_checked_exception() {
		var v1 = "public class A extends IllegalArgumentException {}";
		var v2 = "public class A extends java.io.IOException {}";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A", BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, 1),
			bc("A", "A", BreakingChangeKind.SUPERTYPE_REMOVED, 1));
	}

	@Client("""
		try {
			throw new A();
		} catch (A e) {}
		try {
			throw new A();
		} catch (Exception e) {}""")
	@Test
	void checked_exception_becomes_specific() {
		var v1 = "public class A extends Exception {}";
		var v2 = "public class A extends java.io.IOException {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		try {
			throw new A();
		} catch (java.io.IOException e) {}""")
	@Test
	void specific_exception_becomes_generic() {
		var v1 = "public class A extends java.io.IOException {}";
		var v2 = "public class A extends Exception {}";

		assertBC("A", "A", BreakingChangeKind.SUPERTYPE_REMOVED, 1, buildDiff(v1, v2));
	}

	@Client("new A();")
	@Test
	void class_becomes_runtime_exception() {
		var v1 = "public class A {}";
		var v2 = "public class A extends RuntimeException {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A();")
	@Test
	void class_becomes_throwable() {
		var v1 = "public class A {}";
		var v2 = "public class A extends Throwable {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A();")
	@Test
	void class_becomes_error() {
		var v1 = "public class A {}";
		var v2 = "public class A extends Error {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		try {
			throw new A();
		} catch (Exception e) {}""")
	@Test
	void exception_becomes_throwable() {
		var v1 = "public class A extends Exception {}";
		var v2 = "public class A extends Throwable {}";

		assertBC("A", "A", BreakingChangeKind.SUPERTYPE_REMOVED, 1, buildDiff(v1, v2));
	}

	@Client("""
		try {
			throw new A();
		} catch (Exception e) {}""")
	@Test
	void exception_becomes_error() {
		var v1 = "public class A extends Exception {}";
		var v2 = "public class A extends Error {}";

		assertBC("A", "A", BreakingChangeKind.SUPERTYPE_REMOVED, 1, buildDiff(v1, v2));
	}
}
