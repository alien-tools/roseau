package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;
import org.xmlet.htmlapifaster.A;

import java.io.IOException;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
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

		assertBC("A", BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, 1, buildDiff(v1, v2));
	}

	@Client("throw new A();")
	@Test
	void specific_unchecked_exception_becomes_specific_checked_exception() {
		var v1 = "public class A extends IllegalArgumentException {}";
		var v2 = "public class A extends java.io.IOException {}";

		assertBC("A", BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, 1, buildDiff(v1, v2));
	}

	@Client("throw new A();")
	@Test
	void checked_exception_becomes_specific() {
		var v1 = "public class A extends Exception {}";
		var v2 = "public class A extends java.io.IOException {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		try {
			throw new A();
		} catch (IOException e) {}""")
	@Test
	void specific_exception_becomes_generic() {
		var v1 = "public class A extends java.io.IOException {}";
		var v2 = "public class A extends Exception {}";

		assertNoBC(BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, buildDiff(v1, v2));
	}

	@Client("new A();")
	@Test
	void class_becomes_runtime_exception() {
		var v1 = "public class A {}";
		var v2 = "public class A extends RuntimeException {}";

		assertNoBC(buildDiff(v1, v2));
	}
}
