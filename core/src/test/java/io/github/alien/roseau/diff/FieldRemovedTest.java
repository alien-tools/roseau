package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class FieldRemovedTest {
	@Test
	void leaked_public_field_now_private() {
		var v1 = """
			class A {
				public int f1;
			}
			public class B extends A {
				public int f2;
			}""";
		var v2 = """
			class A {
				int f1;
			}
			public class B extends A {
				public int f2;
			}""";

		assertBC("A.f1", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void leaked_public_field_no_longer_leaked() {
		var v1 = """
			class A {
				public int f1;
			}
			public class B extends A {
				public int f2;
			}""";
		var v2 = """
			class A {
				public int f1;
			}
			public class B {
				public int f2;
			}""";

		assertBC("A.f1", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void public_field_removed() {
		var v1 = """
			public class A {
			    public int f;
			}""";
		var v2 = "public class A {}";

		assertBC("A.f", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void static_field_removed() {
		var v1 = """
			public class A {
			    public static int f;
			}""";
		var v2 = "public class A {}";

		assertBC("A.f", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void field_now_hidden() {
		var v1 = """
			public class A {
			    public int f;
			}""";
		var v2 = """
			public class A {
			    int f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void field_now_initialized() {
		var v1 = """
			public class A {
				public int f;
			}""";
		var v2 = """
			public class A {
				public int f = 2;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void field_visibility_protected_to_private() {
		var v1 = """
			public class A {
			    protected int f;
			}""";
		var v2 = """
			public class A {
			    private int f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void field_visibility_pkg_private_to_private() {
		var v1 = """
			public class A {
			    int f;
			}""";
		var v2 = """
			public class A {
			    private int f;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void field_visibility_public_to_protected() {
		var v1 = """
			public class A {
			    public int f;
			}""";
		var v2 = """
			public class A {
			    protected int f;
			}""";

		var diff = buildDiff(v1, v2);
		assertNoBC(BreakingChangeKind.FIELD_REMOVED, diff);
		assertBC("A.f", BreakingChangeKind.FIELD_NOW_PROTECTED, 2, diff);
	}

	@Test
	void enum_constant_removed() {
		var v1 = """
			public enum E {
			    X, Y;
			}""";
		var v2 = """
			public enum E {
			    X;
			}""";

		assertBC("E.Y", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void annotation_interface_enum_constant_removed() {
		var v1 = """
			public @interface A {
				E[] value() default E.X;
				enum E { X, Y; }
			}""";
		var v2 = """
			public @interface A {
				E[] value() default E.X;
				enum E { X; }
			}""";

		assertBC("A$E.Y", BreakingChangeKind.FIELD_REMOVED, 3, buildDiff(v1, v2));
	}

	@Test
	void record_field_removed() {
		var v1 = "public record A(int i) {}";
		var v2 = "public record A() {}";

		assertNoBC(BreakingChangeKind.FIELD_REMOVED, buildDiff(v1, v2));
	}
}
