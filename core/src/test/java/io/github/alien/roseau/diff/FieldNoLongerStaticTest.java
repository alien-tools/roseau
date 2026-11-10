package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

// §13.4.10
class FieldNoLongerStaticTest {
	@Client("int i = A.f;")
	@Test
	void field_no_longer_static() {
		var v1 = """
			public class A {
				public static int f;
			}""";
		var v2 = """
			public class A {
				public int f = 0;
			}""";

		assertBC("A", "A.f", BreakingChangeKind.FIELD_NO_LONGER_STATIC, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void private_field_no_longer_static() {
		var v1 = """
			public class A {
				private static int f;
			}""";
		var v2 = """
			public class A {
				private int f = 0;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("int i = A.X;")
	@Test
	void constant_no_longer_static() {
		var v1 = """
			public class A {
				public static final int X = 1;
			}""";
		var v2 = """
			public class A {
				public final int X = 1;
			}""";

		assertBC("A", "A.X", BreakingChangeKind.FIELD_CONSTANT_NO_LONGER_STATIC, 2, buildDiff(v1, v2));
	}

	@Client("int i = A.X;")
	@Test
	void non_constant_no_longer_static() {
		var v1 = """
			public class A {
				public static final int X = Integer.parseInt("1");
			}""";
		var v2 = """
			public class A {
				public final int X = Integer.parseInt("1");
			}""";

		assertBC("A", "A.X", BreakingChangeKind.FIELD_NO_LONGER_STATIC, 2, buildDiff(v1, v2));
	}
}
