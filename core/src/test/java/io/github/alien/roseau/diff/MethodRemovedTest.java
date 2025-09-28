package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodRemovedTest {
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

		assertBC("B", "A.m", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new B() {
			@Override protected void m() {}
		};""")
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

		assertBC("B", "A.m", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("B", "A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m1();")
	@Test
	void public_method_removed() {
		var v1 = """
			public class A {
			    public void m1() {}
			}""";
		var v2 = "public class A {}";

		assertBC("A", "A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("A", "A.m1", BreakingChangeKind.METHOD_REMOVED, 3, buildDiff(v1, v2));
	}

	@Client("A.m1();")
	@Test
	void static_method_removed() {
		var v1 = """
			public class A {
			    public static void m1() {}
			}""";
		var v2 = "public class A {}";

		assertBC("A", "A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new I(){}.m1();")
	@Test
	void default_method_removed_in_interface() {
		var v1 = """
			public interface I {
			    default void m1() {}
			}""";
		var v2 = "public interface I {}";

		assertBC("I", "I.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("A", "A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("I", "I.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("A", "A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A() {
			@Override protected void m1() {}
		};""")
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

		assertBC("A", "A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(null, 1);")
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

		assertBC("A", "A.m", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("@A(0) int i;")
	@Test
	void annotation_method_removed() {
		var v1 = """
			public @interface A {
				int value();
			}""";
		var v2 = "public @interface A {}";

		assertBC("A", "A.value", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
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

		assertBC("A", "A.m", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("new A(0, 0f); // Can't really test f()")
	@Test
	void record_getter_removed() {
		var v1 = "public record A(int i, float f) {}";
		var v2 = "public record A(int i) {}";

		assertBC("A", "A.f", BreakingChangeKind.METHOD_REMOVED, -1, buildDiff(v1, v2));
	}
}
