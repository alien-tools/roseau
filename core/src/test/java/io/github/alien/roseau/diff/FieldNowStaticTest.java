package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

// §13.4.10
class FieldNowStaticTest {
	@Client("int i = new A().f;")
	@Test
	void field_now_static() {
		var v1 = """
			public class A {
				public int f;
			}""";
		var v2 = """
			public class A {
				public static int f = 0;
			}""";

		assertBC("A", "A.f", BreakingChangeKind.FIELD_NOW_STATIC, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void private_field_now_static() {
		var v1 = """
			public class A {
				private int f;
			}""";
		var v2 = """
			public class A {
				private static int f = 0;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
