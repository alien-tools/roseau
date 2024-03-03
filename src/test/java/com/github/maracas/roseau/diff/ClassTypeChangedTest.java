package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class ClassTypeChangedTest {
	@Test
	void class_to_interface() {
		String v1 = "public class A {}";
		String v2 = "public interface A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void class_to_record() {
		String v1 = "public class A {}";
		String v2 = "public record A() {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void class_to_enum() {
		String v1 = "public class A {}";
		String v2 = "public enum A { INSTANCE; }";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void interface_to_class() {
		String v1 = "public interface A {}";
		String v2 = "public class A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void interface_to_record() {
		String v1 = "public interface A {}";
		String v2 = "public record A() {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void interface_to_enum() {
		String v1 = "public interface A {}";
		String v2 = "public enum A { INSTANCE; }";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void record_to_class() {
		String v1 = "public record A() {}";
		String v2 = "public class A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void record_to_interface() {
		String v1 = "public record A() {}";
		String v2 = "public interface A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void enum_to_class() {
		String v1 = "public enum A { INSTANCE; }";
		String v2 = "public class A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void enum_to_interface() {
		String v1 = "public enum A { INSTANCE; }";
		String v2 = "public interface A {}";

		assertBC("A", BreakingChangeKind.CLASS_TYPE_CHANGED, 1, buildDiff(v1, v2));
	}
}
