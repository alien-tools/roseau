package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertBC;
import static com.github.maracas.roseau.TestUtils.buildDiff;

class TypeFormalTypeParameterRemovedTest {
	@Test
	void first_param_added() {
		String v1 = "public class A<T> {}";
		String v2 = "public class A {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, 1, buildDiff(v1, v2));
	}

	@Test
	void second_param_added() {
		String v1 = "public class A<T, U> {}";
		String v2 = "public class A<T> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, 1, buildDiff(v1, v2));
	}
}
