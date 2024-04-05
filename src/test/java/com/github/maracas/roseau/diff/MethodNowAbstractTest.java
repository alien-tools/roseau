package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class MethodNowAbstractTest {
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

		assertBC("A.m", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2, buildDiff(v1, v2));
	}

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

		assertBC("I.m", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2, buildDiff(v1, v2));
	}

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
      public class B extends A {}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_NOW_ABSTRACT, 5, buildDiff(v1, v2));
	}
}
