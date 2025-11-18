package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class TypeNewAbstractMethodTest {
	@Client("A a = new A() {};")
	@Test
	void method_abstract_added_to_class() {
		var v1 = "public abstract class A {}";
		var v2 = """
			public abstract class A {
				public abstract void m();
			}""";

		assertBC("A", "A", BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, 1, buildDiff(v1, v2));
	}

	@Client("B b = new B() {};")
	@Test
	void method_abstract_added_to_class_indirect() {
		var v1 = """
			public abstract class A {}
			public abstract class B extends A {}""";
		var v2 = """
			public abstract class A {
				public abstract void m();
			}
			public abstract class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A", BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, 1),
			bc("B", "B", BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, 1));
	}

	@Client("I i = new I() {};")
	@Test
	void method_added_to_interface() {
		var v1 = "public interface I {}";
		var v2 = """
			public interface I {
				void m();
			}""";

		assertBC("I", "I", BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, 1, buildDiff(v1, v2));
	}

	@Client("J j = new J() {};")
	@Test
	void method_added_to_interface_indirect() {
		var v1 = """
			public interface I {}
			public interface J extends I {}""";
		var v2 = """
			public interface I { void m(); }
			public interface J extends I {}""";

		assertBCs(buildDiff(v1, v2),
			bc("I", "I", BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, 1),
			bc("J", "J", BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, 1));
	}
}
