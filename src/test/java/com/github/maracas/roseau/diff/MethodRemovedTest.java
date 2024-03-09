package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class MethodRemovedTest {
	@Test
	void leaked_public_method_now_private() {
		String v1 = """
			class A {
				public void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""";
		String v2 = """
			class A {
				void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""";

		assertBC("A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void leaked_public_method_no_longer_leaked() {
		String v1 = """
			class A {
				public void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""";
		String v2 = """
			class A {
				public void m1() {}
			}
			public class B {
				public void m2() {}
			}""";

		assertBC("A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void public_method_removed() {
		String v1 = """
			public class A {
			    public void m1() {}
			}""";
		String v2 = "public class A {}";

		assertBC("A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void overloaded_method_removed() {
		String v1 = """
			public class A {
			    public void m1() {}
			    public void m1(int i) {}
			}""";
		String v2 = """
			public class A {
			    public void m1() {}
			}""";

		assertBC("A.m1", BreakingChangeKind.METHOD_REMOVED, 3, buildDiff(v1, v2));
	}

	@Test
	void static_method_removed() {
		String v1 = """
			public class A {
			    public static void m1() {}
			}""";
		String v2 = "public class A {}";

		assertBC("A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void default_method_removed_in_interface() {
		String v1 = """
			public interface I {
			    default void m1() {}
			}""";
		String v2 = "public interface I {}";

		assertBC("I.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void method_visibility_reduced_from_public_to_package_private() {
		String v1 = """
			public class A {
			    public void m1() {}
			}""";
		String v2 = """
			public class A {
			    void m1() {}
			}""";

		assertBC("A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void overridden_method_removed_from_subclass() {
		String v1 = """
			public class A {
			    public void m1() {}
			}
			public class B extends A {
			    public void m1() {}
			}""";
		String v2 = """
			public class A {
			    public void m1() {}
			}
			public class B extends A {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void interface_method_removed_affecting_implementer() {
		String v1 = """
			public interface I {
			    void m1();
			}
			public class A implements I {
			    public void m1() {}
			}""";
		String v2 = """
			public interface I {}
			public class A implements I {}""";

		assertBC("I.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void method_parameters_changed() {
		String v1 = """
			public class A {
			    public void m1(int x, String y) {}
			}""";
		String v2 = """
			public class A {
			  public void m1(int x) {}
			}""";

		assertBC("A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void method_visibility_protected_to_private() {
		String v1 = """
			public class A {
			    protected void m1() {}
			}""";
		String v2 = """
			public class A {
			    private void m1() {}
			}""";

		assertBC("A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}
}
