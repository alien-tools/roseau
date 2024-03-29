package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class TypeFormalTypeParameterAddedTest {
	@Test
	void first_param_added() {
		var v1 = "public class A {}";
		var v2 = "public class A<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void second_param_added() {
		var v1 = "public class A<T> {}";
		var v2 = "public class A<T, U> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, 1, buildDiff(v1, v2));
	}
}
