package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class MethodAbstractAddedToClassTest {
	@Test
	void method_abstract_added_to_class() {
		var v1 = "public abstract class A {}";
		var v2 = """
			public abstract class A {
				public abstract void m();
			}""";

		assertBC("A", BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, 1, buildDiff(v1, v2));
	}

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
		assertBC("A", BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, 1, diff);
		assertBC("B", BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, 2, diff);
	}
}
