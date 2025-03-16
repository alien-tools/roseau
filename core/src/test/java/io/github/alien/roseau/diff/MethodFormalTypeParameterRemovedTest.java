package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodFormalTypeParameterRemovedTest {
	@Test
	void first_param_removed() {
		var v1 = """
			public class A {
				public <T> void m() {}
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void second_param_removed() {
		var v1 = """
			public class A {
				public <T, U> void m() {}
			}""";
		var v2 = """
			public class A {
				public <T> void m() {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, 2, buildDiff(v1, v2));
	}
}
