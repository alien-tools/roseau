package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodNowAbstractTest {
	@Client("new A(){};")
	@Test
	void method_now_abstract() {
		var v1 = """
			public abstract class A {
				public void m() {}
			}""";
		var v2 = """
			public abstract class A {
				public abstract void m();
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2, buildDiff(v1, v2));
	}

	@Client("new I(){};")
	@Test
	void default_now_abstract() {
		var v1 = """
			public interface I {
				default void m() {}
			}""";
		var v2 = """
			public interface I {
				void m();
			}""";

		assertBC("I", "I.m()", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2, buildDiff(v1, v2));
	}

	@Client("""
		new I() {
			@Override public void m() {}
		};""")
	@Test
	void implicitly_abstract_to_abstract() {
		var v1 = """
			public interface I {
				void m();
			}""";
		var v2 = """
			public interface I {
				abstract void m();
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A(){};")
	@Test
	void method_becomes_abstract_in_superclass_affecting_subclass() {
		var v1 = """
			public abstract class A {
			  public void m() {}
			}
			public class B extends A {}""";

		var v2 = """
			public abstract class A {
			  public abstract void m();
			}
			public class B extends A {
			  public void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2, buildDiff(v1, v2));
	}

	@Client("new A(){};")
	@Test
	void abstract_class_implements_interface_method_as_abstract() {
		var v1 = """
			public interface I {
			  void m();
			}
			public abstract class A implements I {
			  public void m() {}
			}""";

		var v2 = """
			public interface I {
			  void m();
			}
			public abstract class A implements I {
			  public abstract void m();
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2, buildDiff(v1, v2));
	}

	@Client("B b = new B() {};")
	@Test
	void super_concrete_method_becomes_abstract_explicit() {
		var v1 = """
			public class A {
				public void m() {}
			}
			public abstract class B extends A {
				@Override public void m() {}
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}
			public abstract class B extends A {
				@Override abstract public void m();
			}""";

		assertBC("B", "B.m()", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2, buildDiff(v1, v2));
	}

	@Client("B b = new B() {};")
	@Test
	void super_concrete_method_becomes_abstract_implicit() {
		var v1 = """
			public class A {
				public void m() {}
			}
			public abstract class B extends A {}""";
		var v2 = """
			public class A {
				public void m() {}
			}
			public abstract class B extends A {
				@Override abstract public void m();
			}""";

		assertBC("B", "A.m()", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2, buildDiff(v1, v2));
	}
}
