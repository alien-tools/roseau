package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class ExecutableRemovedTest {
	@Client("new B().m();")
	@Test
	void leaked_public_method_now_private() {
		var v1 = """
			class A {
				public void m() {}
			}
			public class B extends A {}""";
		var v2 = """
			class A {
				private void m() {}
			}
			public class B extends A {}""";

		assertBC("B", "A.m()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new B() {
			@Override protected void m() {
				super.m();
			}
		}.m();""")
	@Test
	void leaked_protected_method_now_private() {
		var v1 = """
			class A {
				protected void m() {}
			}
			public class B extends A {}""";
		var v2 = """
			class A {
				private void m() {}
			}
			public class B extends A {}""";

		assertBC("B", "A.m()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("// Can't extend or use")
	@Test
	void leaked_protected_method_now_private_in_final() {
		var v1 = """
			class A {
				protected void m() {}
			}
			public final class B extends A {}""";
		var v2 = """
			class A {
				private void m() {}
			}
			public final class B extends A {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new B().m1();")
	@Test
	void leaked_methods_no_longer_leaked() {
		var v1 = """
			class A {
				public void m1() {}
				protected void m2() {}
			}
			public class B extends A {
				public void m3() {}
			}""";
		var v2 = """
			class A {
				void m1() {}
				void m2() {}
			}
			public class B {
				public void m3() {}
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("B", "A.m1()", BreakingChangeKind.EXECUTABLE_REMOVED, 2),
			bc("B", "A.m2()", BreakingChangeKind.EXECUTABLE_REMOVED, 3));
	}

	@Client("new A().m1();")
	@Test
	void public_method_removed() {
		var v1 = """
			public class A {
			    public void m1() {}
			}""";
		var v2 = "public class A {}";

		assertBC("A", "A.m1()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m1(0);")
	@Test
	void overloaded_method_removed() {
		var v1 = """
			public class A {
			    public void m1() {}
			    public void m1(int i) {}
			}""";
		var v2 = """
			public class A {
			    public void m1() {}
			}""";

		assertBC("A", "A.m1(int)", BreakingChangeKind.EXECUTABLE_REMOVED, 3, buildDiff(v1, v2));
	}

	@Client("A.m1();")
	@Test
	void static_method_removed() {
		var v1 = """
			public class A {
			    public static void m1() {}
			}""";
		var v2 = "public class A {}";

		assertBC("A", "A.m1()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("B.m();")
	@Test
	void inherited_static_method_removed() {
		var v1 = """
			class A {
			    public static void m() {}
			}
			public class B extends A {}""";
		var v2 = """
			class A {}
			public class B extends A {}""";

		assertBC("B", "A.m()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new I(){}.m1();")
	@Test
	void default_method_removed_in_interface() {
		var v1 = """
			public interface I {
			    default void m1() {}
			}""";
		var v2 = "public interface I {}";

		assertBC("I", "I.m1()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m1();")
	@Test
	void method_visibility_reduced_from_public_to_package_private() {
		var v1 = """
			public class A {
			    public void m1() {}
			}""";
		var v2 = """
			public class A {
			    void m1() {}
			}""";

		assertBC("A", "A.m1()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new B().m1();")
	@Test
	void overridden_method_removed_from_subclass() {
		var v1 = """
			public class A {
			    public void m1() {}
			}
			public class B extends A {
			    public void m1() {}
			}""";
		var v2 = """
			public class A {
			    public void m1() {}
			}
			public class B extends A {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m1();")
	@Test
	void interface_method_removed_affecting_implementer() {
		var v1 = """
			public interface I {
			    void m1();
			}
			public class A implements I {
			    public void m1() {}
			}""";
		var v2 = """
			public interface I {}
			public class A implements I {}""";

		assertBCs(buildDiff(v1, v2),
			bc("I", "I.m1()", BreakingChangeKind.EXECUTABLE_REMOVED, 2),
			bc("A", "A.m1()", BreakingChangeKind.EXECUTABLE_REMOVED, 2));
	}

	@Client("new A().m1(0, null);")
	@Test
	void method_parameters_changed() {
		var v1 = """
			public class A {
			    public void m1(int x, Object y) {}
			}""";
		var v2 = """
			public class A {
			  public void m1(int x) {}
			}""";

		assertBC("A", "A.m1(int,java.lang.Object)", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A() {
			@Override protected void m1() {
				super.m1();
			}
		}.m1();""")
	@Test
	void method_visibility_protected_to_private() {
		var v1 = """
			public class A {
			    protected void m1() {}
			}""";
		var v2 = """
			public class A {
			    private void m1() {}
			}""";

		assertBC("A", "A.m1()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A().m(null, 1);
		new A() { @Override public void m(Object o, int i) {} };
		""")
	@Test
	void method_now_varargs() {
		var v1 = """
			public class A {
			    public void m(Object o, int i) {}
			}""";
		var v2 = """
			public class A {
			    public void m(Object o, int... i) {}
			}""";

		assertBC("A", "A.m(java.lang.Object,int)", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("@A(0) int i;")
	@Test
	void annotation_method_removed() {
		var v1 = """
			public @interface A {
				int value();
			}""";
		var v2 = "public @interface A {}";

		assertBC("A", "A.value()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new A(0).m();")
	@Test
	void record_method_removed() {
		var v1 = """
			public record A(int i) {
			    public void m() {}
			}""";
		var v2 = """
			public record A(int i) {
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new A(0, 0f); // Can't really test f()")
	@Test
	void record_getter_removed() {
		var v1 = "public record A(int i, float f) {}";
		var v2 = "public record A(int i) {}";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.<init>(int,float)", BreakingChangeKind.EXECUTABLE_REMOVED, -1),
			bc("A", "A.f()", BreakingChangeKind.EXECUTABLE_REMOVED, -1));
	}

	@Client("A a = new A();")
	@Test
	void class_default_constructor_removed() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				public A(int i) {}
			}""";

		assertBC("A", "A.<init>()", BreakingChangeKind.EXECUTABLE_REMOVED, -1, buildDiff(v1, v2));
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
			bc("A", "A.<init>()", BreakingChangeKind.EXECUTABLE_REMOVED, -1),
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

		assertBC("A", "A.<init>(int)", BreakingChangeKind.EXECUTABLE_NOW_PROTECTED, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void class_constructor_now_protected_default() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				protected A() {}
			}""";

		assertBC("A", "A.<init>()", BreakingChangeKind.EXECUTABLE_NOW_PROTECTED, -1, buildDiff(v1, v2));
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
		};
		new B(0);""")
	@Test
	void protected_constructor_removed() {
		var v1 = """
			public class A {
				protected A(int i) {}
			}""";
		var v2 = "public class A {}";

		assertBC("A", "A.<init>(int)", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A(0);")
	@Test
	void record_implicit_constructor_changed() {
		var v1 = "public record A(int i) {}";
		var v2 = "public record A(String s) {}";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.<init>(int)", BreakingChangeKind.EXECUTABLE_REMOVED, -1),
			bc("A", "A.i()", BreakingChangeKind.EXECUTABLE_REMOVED, -1));
	}

	@Client("A a = new A(0);")
	@Test
	void record_implicit_constructor_changed_add() {
		var v1 = "public record A(int i) {}";
		var v2 = "public record A(int i, float f) {}";

		assertBC("A", "A.<init>(int)", BreakingChangeKind.EXECUTABLE_REMOVED, -1, buildDiff(v1, v2));
	}

	@Client("A a = new A(0, 0f);")
	@Test
	void record_implicit_constructor_changed_remove() {
		var v1 = "public record A(int i, float f) {}";
		var v2 = "public record A(int i) {}";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.<init>(int,float)", BreakingChangeKind.EXECUTABLE_REMOVED, -1),
			bc("A", "A.f()", BreakingChangeKind.EXECUTABLE_REMOVED, -1));
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

		assertBC("A", "A.<init>()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("A", "A.<init>(boolean)", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("A", "A.<init>(int)", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("A", "A.<init>(boolean)", BreakingChangeKind.EXECUTABLE_REMOVED, 3, buildDiff(v1, v2));
	}

	@Client("new A().m();")
	@Test
	void public_to_protected() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				protected void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.EXECUTABLE_NOW_PROTECTED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A() {
			@Override protected void m() {
				super.m();
			}
		}.m();""")
	@Test
	void protected_to_package_private() {
		var v1 = """
			public class A {
				protected void m() {}
			}""";
		var v2 = """
			public class A {
				void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.EXECUTABLE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void package_private_to_private() {
		var v1 = """
			public class A {
				void m() {}
			}""";
		var v2 = """
			public class A {
				private void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
