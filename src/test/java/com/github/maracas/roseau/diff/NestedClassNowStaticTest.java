package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertBC;
import static com.github.maracas.roseau.TestUtils.assertNoBC;
import static com.github.maracas.roseau.TestUtils.buildDiff;

class NestedClassNowStaticTest {
	@Test
	void nested_class_now_static() {
		String v1 = "public class A { public class B {} }";
		String v2 = "public class A { public static class B {} }";

		assertBC("A$B", BreakingChangeKind.NESTED_CLASS_NOW_STATIC, 1, buildDiff(v1, v2));
	}

	@Test
	void doubly_nested_class_now_static() {
		String v1 = "public class A { public class B { public class C {} } }";
		String v2 = "public class A { public class B { public static class C {} } }";

		assertBC("A$B$C", BreakingChangeKind.NESTED_CLASS_NOW_STATIC, 1, buildDiff(v1, v2));
	}

	@Test
	void nested_class_in_interface_now_static() {
		String v1 = "public interface A { public class B {} }";
		String v2 = "public interface A { public static class B {} }";

		assertNoBC(buildDiff(v1, v2)); // Classes nested within interfaces are implicitly static
	}

	@Test
	void nested_interface_now_static() {
		String v1 = "public class A { public interface B {} }";
		String v2 = "public class A { public static interface B {} }";

		assertNoBC(buildDiff(v1, v2)); // Nested interfaces are implicitly static
	}

	@Test
	void nested_enum_now_static() {
		String v1 = "public class A { public enum B {} }";
		String v2 = "public class A { public static enum B {} }";

		assertNoBC(buildDiff(v1, v2)); // Enums nested within classes are implicitly static
	}

	@Test
	void nested_annotation_now_static() {
		String v1 = "public class A { public @interface B {} }";
		String v2 = "public class A { public static @interface B {} }";

		assertNoBC(buildDiff(v1, v2)); // Annotations nested within classes are implicitly static
	}

	@Test
	void nested_record_now_static() {
		String v1 = "public class A { public record B() {} }";
		String v2 = "public class A { public static record B() {} }";

		assertNoBC(buildDiff(v1, v2)); // Record nested within classes are implicitly static
	}
}
