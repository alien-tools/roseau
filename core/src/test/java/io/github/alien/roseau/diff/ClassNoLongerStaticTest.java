package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class ClassNoLongerStaticTest {
	@Client("A.B b = new A.B();")
	@Test
	void nested_class_no_longer_static() {
		var v1 = "public class A { public static class B {} }";
		var v2 = "public class A { public class B {} }";

		assertBC("A$B", "A$B", BreakingChangeKind.CLASS_NO_LONGER_STATIC, 1, buildDiff(v1, v2));
	}

	@Client("A.B.C c = new A.B.C();")
	@Test
	void doubly_nested_class_no_longer_static() {
		var v1 = "public class A { public class B { public static class C {} } }";
		var v2 = "public class A { public class B { public class C {} } }";

		assertBC("A$B$C", "A$B$C", BreakingChangeKind.CLASS_NO_LONGER_STATIC, 1, buildDiff(v1, v2));
	}

	@Client("A.B b = new A.B();")
	@Test
	void nested_class_in_interface_no_longer_static() {
		var v1 = "public interface A { public static class B {} }";
		var v2 = "public interface A { public class B {} }";

		assertNoBC(buildDiff(v1, v2)); // Classes nested within interfaces are implicitly static
	}

	@Client("A.B b = new A.B(){};")
	@Test
	void nested_interface_no_longer_static() {
		var v1 = "public class A { public static interface B {} }";
		var v2 = "public class A { public interface B {} }";

		assertNoBC(buildDiff(v1, v2)); // Nested interfaces are implicitly static
	}

	@Client("A.B b;")
	@Test
	void nested_enum_no_longer_static() {
		var v1 = "public class A { public static enum B {} }";
		var v2 = "public class A { public enum B {} }";

		assertNoBC(buildDiff(v1, v2)); // Enums nested within classes are implicitly static
	}

	@Client("@A.B int i;")
	@Test
	void nested_annotation_no_longer_static() {
		var v1 = "public class A { public static @interface B {} }";
		var v2 = "public class A { public @interface B {} }";

		assertNoBC(buildDiff(v1, v2)); // Annotations nested within classes are implicitly static
	}

	@Client("A.B b = new A.B();")
	@Test
	void nested_record_no_longer_static() {
		var v1 = "public class A { public static record B() {} }";
		var v2 = "public class A { public record B() {} }";

		assertNoBC(buildDiff(v1, v2)); // Record nested within classes are implicitly static
	}
}
