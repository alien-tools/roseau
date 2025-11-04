package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class TypeFormalTypeParameterRemovedTest {
	@Client("A<String> a;")
	@Test
	void first_param_removed() {
		var v1 = "public class A<T> {}";
		var v2 = "public class A {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, 1, buildDiff(v1, v2));
	}

	@Client("A<String, String> a;")
	@Test
	void second_param_removed() {
		var v1 = "public class A<T, U> {}";
		var v2 = "public class A<T> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, 1, buildDiff(v1, v2));
	}
}
