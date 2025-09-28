package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodAddedToInterfaceTest {
	@Client("I i = new I() {};")
	@Test
	void method_added_to_interface() {
		var v1 = "public interface I {}";
		var v2 = """
			public interface I {
				void m();
			}""";

		assertBC("I", "I", BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, 1, buildDiff(v1, v2));
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
			bc("I", "I", BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, 1),
			bc("J", "J", BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, 1));
	}
}
