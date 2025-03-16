package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class ClassTypeChangedTest {
	@Test
	void class_to_interface() {
		var v1 = "public class A {}";
		var v2 = "public interface A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void class_to_record() {
		var v1 = "public class A {}";
		var v2 = "public record A() {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void class_to_enum() {
		var v1 = "public class A {}";
		var v2 = "public enum A { INSTANCE; }";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void interface_to_class() {
		var v1 = "public interface A {}";
		var v2 = "public class A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void interface_to_record() {
		var v1 = "public interface A {}";
		var v2 = "public record A() {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void interface_to_enum() {
		var v1 = "public interface A {}";
		var v2 = "public enum A { INSTANCE; }";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void record_to_class() {
		var v1 = "public record A() {}";
		var v2 = "public class A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void record_to_interface() {
		var v1 = "public record A() {}";
		var v2 = "public interface A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void enum_to_class() {
		var v1 = "public enum A { INSTANCE; }";
		var v2 = "public class A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void enum_to_interface() {
		var v1 = "public enum A { INSTANCE; }";
		var v2 = "public interface A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}
}
