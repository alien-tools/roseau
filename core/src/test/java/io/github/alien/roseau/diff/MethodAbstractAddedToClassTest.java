package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodAbstractAddedToClassTest {
	@Client("A a = new A() {};")
	@Test
	void method_abstract_added_to_class() {
		var v1 = "public abstract class A {}";
		var v2 = """
			public abstract class A {
				public abstract void m();
			}""";

		assertBC("A", "A", BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, 1, buildDiff(v1, v2));
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

		var diff = buildDiff(v1, v2);
		assertBC("A", "A", BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, 1, diff);
		assertBC("B", "B", BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, 1, diff);
	}
}
