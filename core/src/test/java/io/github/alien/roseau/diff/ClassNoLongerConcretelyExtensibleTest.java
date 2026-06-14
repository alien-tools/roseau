package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class ClassNoLongerConcretelyExtensibleTest {
	@Client("abstract class B extends A { @Override public void m() {} }")
	@Test
	void now_abstract_method_in_unconcretizable_class() {
		var v1 = """
			public abstract class A {
				public void m() {}
				abstract void n();
			}""";
		var v2 = """
			public abstract class A {
				public abstract void m();
				abstract void n();
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("class B extends A { @Override public void m() {} }")
	@Test
	void making_package_private_method_abstract_in_concretizable_class() {
		var v1 = """
			public abstract class A {
				public abstract void m();
				void n() {}
			}""";
		var v2 = """
			public abstract class A {
				public abstract void m();
				abstract void n();
			}""";

		assertBC("A", "A", BreakingChangeKind.CLASS_NO_LONGER_CONCRETELY_EXTENSIBLE, 1, buildDiff(v1, v2));
	}

	@Client("""
		class C extends B {
			@Override public void m() {}
			@Override public void n() {}
		}""")
	@Test
	void abstract_method_no_longer_overridden() {
		var v1 = """
			abstract class A {
				abstract void m();
			}
			
			public abstract class B extends A {
				public abstract void n();
				public void m() {}
			}""";
		var v2 = """
			abstract class A {
				abstract void m();
			}
			
			public abstract class B extends A {
				public abstract void n();
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("B", "B", BreakingChangeKind.CLASS_NO_LONGER_CONCRETELY_EXTENSIBLE, 1),
			bc("B", "B.m()", BreakingChangeKind.EXECUTABLE_REMOVED, 3));
	}

	@Client("abstract class B extends A { @Override public void m() {} }")
	@Test
	void new_abstract_method_in_unconcretizable_class() {
		var v1 = """
			public abstract class A {
				public abstract void m();
				abstract void n();
			}""";
		var v2 = """
			public abstract class A {
				public abstract void m();
				abstract void n();
				public abstract void o();
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("abstract class C extends B {}")
	@Test
	void new_abstract_method_in_unconcretizable_class_inherited() {
		var v1 = """
			abstract class A {
				abstract void m();
			}
			
			public abstract class B extends A {
				public abstract void n();
			}""";
		var v2 = """
			abstract class A {
				abstract void m();
			}
			
			public abstract class B extends A {
				public abstract void n();
				public abstract void o();
			}""";
	}

	@Client("class B extends A { @Override public void m() {} }")
	@Test
	void adding_package_private_abstract_method_to_concretizable_class() {
		var v1 = """
			public abstract class A {
				public abstract void m();
			}""";
		var v2 = """
			public abstract class A {
				public abstract void m();
				abstract void n();
			}""";

		assertBC("A", "A", BreakingChangeKind.CLASS_NO_LONGER_CONCRETELY_EXTENSIBLE, 1, buildDiff(v1, v2));
	}
}
