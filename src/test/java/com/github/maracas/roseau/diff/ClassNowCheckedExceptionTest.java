package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertBC;
import static com.github.maracas.roseau.TestUtils.assertNoBC;
import static com.github.maracas.roseau.TestUtils.buildDiff;

class ClassNowCheckedExceptionTest {
	@Test
	void class_becomes_generic_checked_exception() {
		String v1 = "public class A {}";
		String v2 = "public class A extends Exception {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_becomes_specific_checked_exception() {
		String v1 = "public class A {}";
		String v2 = "public class A extends java.io.IOException {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void unchecked_exception_becomes_checked_exception() {
		String v1 = "public class A extends RuntimeException {}";
		String v2 = "public class A extends Exception {}";

		assertBC("A", BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, 1, buildDiff(v1, v2));
	}

	@Test
	void specific_unchecked_exception_becomes_specific_checked_exception() {
		String v1 = "public class A extends IllegalArgumentException {}";
		String v2 = "public class A extends java.io.IOException {}";

		assertBC("A", BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, 1, buildDiff(v1, v2));
	}

	@Test
	void checked_exception_becomes_specific() {
		String v1 = "public class A extends Exception {}";
		String v2 = "public class A extends java.io.IOException {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void specific_exception_becomes_generic() {
		String v1 = "public class A extends java.io.IOException {}";
		String v2 = "public class A extends Exception {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_becomes_runtime_exception() {
		String v1 = "public class A {}";
		String v2 = "public class A extends RuntimeException {}";

		assertNoBC(buildDiff(v1, v2));
	}
}
