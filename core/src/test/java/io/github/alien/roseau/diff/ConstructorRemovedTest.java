package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class ConstructorRemovedTest {
	@Client("A a = new A();")
	@Test
	void class_default_constructor_removed() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				public A(int i) {}
			}""";

		assertBC("A", "A.<init>()", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1, buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void class_default_constructor_now_explicit() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				public A() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void class_explicit_constructor_now_default() {
		var v1 = """
			public class A {
				public A() {}
			}""";
		var v2 = "public class A {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void class_constructor_now_private() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				private A() {}
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.<init>()", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1),
			bc("A", "A", BreakingChangeKind.CLASS_NOW_ABSTRACT, 1),
			bc("A", "A", BreakingChangeKind.CLASS_NOW_FINAL, 1));
	}

	@Client("A a = new A(0);")
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

		assertBC("A", "A.<init>(int)", BreakingChangeKind.CONSTRUCTOR_NOW_PROTECTED, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void class_constructor_now_protected_default() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				protected A() {}
			}""";

		assertBC("A", "A.<init>()", BreakingChangeKind.CONSTRUCTOR_NOW_PROTECTED, -1, buildDiff(v1, v2));
	}

	@Client("""
		class B extends A {
			B() {
				super();
			}
		};""")
	@Test
	void default_protected_constructor_removed() {
		var v1 = """
			public class A {
				protected A() {}
			}""";
		var v2 = "public class A {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		class B extends A {
			B(int i) {
				super(i);
			}
		};""")
	@Test
	void protected_constructor_removed() {
		var v1 = """
			public class A {
				protected A(int i) {}
			}""";
		var v2 = "public class A {}";

		assertBC("A", "A.<init>(int)", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A(0);")
	@Test
	void record_implicit_constructor_changed() {
		var v1 = "public record A(int i) {}";
		var v2 = "public record A(String s) {}";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.<init>(int)", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1),
			bc("A", "A.i()", BreakingChangeKind.METHOD_REMOVED, -1));
	}

	@Client("A a = new A(0);")
	@Test
	void record_implicit_constructor_changed_add() {
		var v1 = "public record A(int i) {}";
		var v2 = "public record A(int i, float f) {}";

		assertBC("A", "A.<init>(int)", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1, buildDiff(v1, v2));
	}

	@Client("A a = new A(0, 0f);")
	@Test
	void record_implicit_constructor_changed_remove() {
		var v1 = "public record A(int i, float f) {}";
		var v2 = "public record A(int i) {}";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.<init>(int,float)", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1),
			bc("A", "A.f()", BreakingChangeKind.METHOD_REMOVED, -1));
	}

	@Client("A a = new A();")
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

		assertBC("A", "A.<init>()", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A(true);")
	@Test
	void record_explicit_constructor_changed() {
		var v1 = """
			public record A(int i) {
				public A(boolean f) {
					this(0);
				}
			}""";
		var v2 = """
			public record A(int i) {
				public A(String s) {
					this(0);
				}
			}""";

		assertBC("A", "A.<init>(boolean)", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A(0);")
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

	@Client("A a = new A(0);")
	@Test
	void class_constructor_changed() {
		var v1 = """
			public class A {
				public A(int i) {}
			}""";
		var v2 = """
			public class A {
				public A(String s) {}
			}""";

		assertBC("A", "A.<init>(int)", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A(true);")
	@Test
	void overloaded_constructor_removed() {
		var v1 = """
			public class A {
				public A(int i) {}
				public A(boolean b) {}
			}""";
		var v2 = """
			public class A {
				public A(int i) {}
			}""";

		assertBC("A", "A.<init>(boolean)", BreakingChangeKind.CONSTRUCTOR_REMOVED, 3, buildDiff(v1, v2));
	}
}
