package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodAddedToInterfaceTest {
	@Test
	void method_added_to_interface() {
		var v1 = "public interface I {}";
		var v2 = """
			public interface I {
				void m();
			}""";

		assertBC("I", BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, 1, buildDiff(v1, v2));
	}

	@Test
	void method_added_to_interface_indirect() {
		var v1 = """
			public interface I {}
			public interface J extends I {}""";
		var v2 = """
			public interface I { void m(); }
			public interface J extends I {}""";

		var diff = buildDiff(v1, v2);
		assertBC("I", BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, 1, diff);
		assertBC("J", BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, 1, diff);
	}
}
