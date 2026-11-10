package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.ApiDifferType;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
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

	@Client("int i = new A().f;")
	@Test
	void instance_constant_now_static() {
		var v1 = """
			public class A {
				public final int f = 1;
			}""";
		var v2 = """
			public class A {
				public static final int f = 1;
			}""";

		// Compiled clients inline the constant (JLS §13.1), and recompiled ones may access a static field through an
		// instance
		assertNoBC(buildDiff(v1, v2));
	}

	@Client("Integer i = new A().f;")
	@Test
	void instance_final_non_constant_now_static() {
		var v1 = """
			public class A {
				public final Integer f = 1;
			}""";
		var v2 = """
			public class A {
				public static final Integer f = 1;
			}""";

		// Not a constant variable: compiled clients use getfield and fail with IncompatibleClassChangeError
		assertBC("A", "A.f", BreakingChangeKind.FIELD_NOW_STATIC, 2, buildDiff(v1, v2));
	}

	@Client("int i = new A().f;")
	@Test
	void field_now_static_constant() {
		var v1 = """
			public class A {
				public int f;
			}""";
		var v2 = """
			public class A {
				public static final int f = 1;
			}""";

		// Only the old declaration matters: clients compiled against v1 read the field with getfield
		assertBCs(buildDiff(v1, v2),
			bc("A", "A.f", BreakingChangeKind.FIELD_NOW_STATIC, 2),
			bc("A", "A.f", BreakingChangeKind.FIELD_NOW_FINAL, 2));
	}

	@Client("int i = new A().c + new A().n;")
	@ParameterizedTest
	@EnumSource(ApiDifferType.class)
	void constant_and_non_constant_now_static(ApiDifferType differ) {
		var v1 = """
			public class A {
				public final int c = 1;
				public final int n = Integer.parseInt("1");
			}""";
		var v2 = """
			public class A {
				public static final int c = 1;
				public static final int n = Integer.parseInt("1");
			}""";

		assertBC("A", "A.n", BreakingChangeKind.FIELD_NOW_STATIC, differ == ApiDifferType.ASM ? -1 : 3,
			differ.diff(v1, v2));
	}
}
