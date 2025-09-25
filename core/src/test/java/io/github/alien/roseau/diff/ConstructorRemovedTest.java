package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class ConstructorRemovedTest {
	@Test
	void class_default_constructor_removed() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				public A(int i) {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1, buildDiff(v1, v2));
	}

	@Test
	void class_default_constructor_now_explicit() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				public A() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_explicit_constructor_now_default() {
		var v1 = """
			public class A {
				public A() {}
			}""";
		var v2 = "public class A {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_constructor_now_private() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				private A() {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1, buildDiff(v1, v2));
	}

	@Test
	void class_constructor_now_protected() {
		var v1 = """
			public class A {
				public A(int i) {}
			}""";
		var v2 = """
			public class A {
				protected A(int i) {}
			}""";

		var diff = buildDiff(v1, v2);
		assertNoBC(BreakingChangeKind.CONSTRUCTOR_REMOVED, diff);
		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_NOW_PROTECTED, 2, diff);
	}

	@Test
	void class_constructor_now_protected_default() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				protected A() {}
			}""";

		var diff = buildDiff(v1, v2);
		assertNoBC(BreakingChangeKind.CONSTRUCTOR_REMOVED, diff);
		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_NOW_PROTECTED, -1, diff);
	}

	@Test
	void record_implicit_constructor_changed() {
		var v1 = "public record A(int i) {}";
		var v2 = "public record A(float f) {}";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1, buildDiff(v1, v2));
	}

	@Test
	void record_implicit_constructor_changed_add() {
		var v1 = "public record A(int i) {}";
		var v2 = "public record A(int i, float f) {}";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1, buildDiff(v1, v2));
	}

	@Test
	void record_implicit_constructor_changed_remove() {
		var v1 = "public record A(int i, float f) {}";
		var v2 = "public record A(int i) {}";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1, buildDiff(v1, v2));
	}

	@Test
	void record_explicit_constructor_removed() {
		var v1 = """
			public record A(int i) {
				public A() {
					this(0);
				}
			}""";
		var v2 = """
			public record A(int i) {
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void record_explicit_constructor_changed() {
		var v1 = """
			public record A(int i) {
				public A(float f) {
					this(0);
				}
			}""";
		var v2 = """
			public record A(int i) {
				public A(String s) {
					this(0);
				}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void record_default_constructor_removed() {
		var v1 = """
			public record A(int i) {
				public A {}
			}""";
		var v2 = """
			public record A(int i) {
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_constructor_changed() {
		var v1 = """
			public class A {
				public A(int i) {}
			}""";
		var v2 = """
			public class A {
				public A(float f) {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void overloaded_constructor_removed() {
		var v1 = """
			public class A {
				public A(int i) {}
				public A(float f) {}
			}""";
		var v2 = """
			public class A {
				public A(int i) {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, 3, buildDiff(v1, v2));
	}
}
