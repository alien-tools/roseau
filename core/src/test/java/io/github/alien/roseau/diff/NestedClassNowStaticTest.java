package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class NestedClassNowStaticTest {
	@Client("new A().new B();")
	@Test
	void nested_class_now_static() {
		var v1 = "public class A { public class B {} }";
		var v2 = "public class A { public static class B {} }";

		assertBC("A$B", "A$B", BreakingChangeKind.NESTED_CLASS_NOW_STATIC, 1, buildDiff(v1, v2));
	}

	@Client("new A().new B().new C();")
	@Test
	void doubly_nested_class_now_static() {
		var v1 = "public class A { public class B { public class C {} } }";
		var v2 = "public class A { public class B { public static class C {} } }";

		assertBC("A$B$C", "A$B$C", BreakingChangeKind.NESTED_CLASS_NOW_STATIC, 1, buildDiff(v1, v2));
	}

	@Client("A.B b;")
	@Test
	void nested_class_in_interface_now_static() {
		var v1 = "public interface A { public class B {} }";
		var v2 = "public interface A { public static class B {} }";

		assertNoBC(buildDiff(v1, v2)); // Classes nested within interfaces are implicitly static
	}

	@Client("A.B b;")
	@Test
	void nested_interface_now_static() {
		var v1 = "public class A { public interface B {} }";
		var v2 = "public class A { public static interface B {} }";

		assertNoBC(buildDiff(v1, v2)); // Nested interfaces are implicitly static
	}

	@Client("A.B b;")
	@Test
	void nested_enum_now_static() {
		var v1 = "public class A { public enum B {} }";
		var v2 = "public class A { public static enum B {} }";

		assertNoBC(buildDiff(v1, v2)); // Enums nested within classes are implicitly static
	}

	@Client("@A.B int a;")
	@Test
	void nested_annotation_now_static() {
		var v1 = "public class A { public @interface B {} }";
		var v2 = "public class A { public static @interface B {} }";

		assertNoBC(buildDiff(v1, v2)); // Annotations nested within classes are implicitly static
	}

	@Client("A.B b;")
	@Test
	void nested_record_now_static() {
		var v1 = "public class A { public record B() {} }";
		var v2 = "public class A { public static record B() {} }";

		assertNoBC(buildDiff(v1, v2)); // Record nested within classes are implicitly static
	}
}
