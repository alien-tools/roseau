package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class FormalTypeParameterAddedTest {
	@Client("A a;")
	@Test
	void class_first_param_added() {
		var v1 = "public class A {}";
		var v2 = "public class A<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String> a;")
	@Test
	void class_second_param_added() {
		var v1 = "public class A<T> {}";
		var v2 = "public class A<T, U> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_ADDED, 1, buildDiff(v1, v2));
	}

	@Client("new A().m();")
	@Test
	void method_first_param_added() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public <T> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().<String>m();")
	@Test
	void method_second_param_added() {
		var v1 = """
			public class A {
				public <T> void m() {}
			}""";
		var v2 = """
			public class A {
				public <T, U> void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.FORMAL_TYPE_PARAMETER_ADDED, 2, buildDiff(v1, v2));
	}
}
