package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class FieldNowProtectedTest {
	@Client("int i = A.X;")
	@Test
	void static_constant_now_protected() {
		var v1 = """
			public class A {
				public static final int X = 1;
			}""";
		var v2 = """
			public class A {
				protected static final int X = 1;
			}""";

		assertBC("A", "A.X", BreakingChangeKind.FIELD_CONSTANT_NOW_PROTECTED, 2, buildDiff(v1, v2));
	}

	@Client("String s = new A().x;")
	@Test
	void instance_constant_now_protected() {
		var v1 = """
			public class A {
				public final String x = "x";
			}""";
		var v2 = """
			public class A {
				protected final String x = "x";
			}""";

		assertBC("A", "A.x", BreakingChangeKind.FIELD_CONSTANT_NOW_PROTECTED, 2, buildDiff(v1, v2));
	}

	@Client("int i = B.X;")
	@Test
	void inherited_constant_now_protected() {
		var v1 = """
			public class A {
				public static final int X = 1;
			}
			public class B extends A {}""";
		var v2 = """
			public class A {
				protected static final int X = 1;
			}
			public class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.X", BreakingChangeKind.FIELD_CONSTANT_NOW_PROTECTED, 2),
			bc("B", "A.X", BreakingChangeKind.FIELD_CONSTANT_NOW_PROTECTED, 2));
	}

	@Client("Integer i = A.X;")
	@Test
	void non_constant_field_now_protected() {
		var v1 = """
			public class A {
				public static final Integer X = 1;
			}""";
		var v2 = """
			public class A {
				protected static final Integer X = 1;
			}""";

		assertBC("A", "A.X", BreakingChangeKind.FIELD_NOW_PROTECTED, 2, buildDiff(v1, v2));
	}
}
