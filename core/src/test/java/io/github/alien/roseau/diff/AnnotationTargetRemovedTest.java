package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class AnnotationTargetRemovedTest {
	@Client("@A(0) int a;")
	@Test
	void annotation_target_changed() {
		var v1 = """
			@java.lang.annotation.Target(java.lang.annotation.ElementType.LOCAL_VARIABLE)
			public @interface A {
				int value();
			}""";
		var v2 = """
			@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
			public @interface A {
				int value();
			}""";

		assertBC("A", "A", BreakingChangeKind.ANNOTATION_TARGET_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("@A(0) int a;")
	@Test
	void annotation_target_removed() {
		var v1 = """
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.METHOD,
				java.lang.annotation.ElementType.LOCAL_VARIABLE
			})
			public @interface A {
				int value();
			}""";
		var v2 = """
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.METHOD
			})
			public @interface A {
				int value();
			}""";

		assertBC("A", "A", BreakingChangeKind.ANNOTATION_TARGET_REMOVED, 6, buildDiff(v1, v2));
	}

	@Client("@A(0) int a;")
	@Test
	void annotation_target_added() {
		var v1 = """
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.LOCAL_VARIABLE
			})
			public @interface A {
				int value();
			}""";
		var v2 = """
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.LOCAL_VARIABLE,
				java.lang.annotation.ElementType.TYPE
			})
			public @interface A {
				int value();
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("@A(0) int a;")
	@Test
	void default_annotation_target_compatible() {
		// Default to all except TYPE_USE
		var v1 = """
			public @interface A {
				int value();
			}""";
		var v2 = """
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.PARAMETER,
				java.lang.annotation.ElementType.CONSTRUCTOR, java.lang.annotation.ElementType.LOCAL_VARIABLE,
				java.lang.annotation.ElementType.ANNOTATION_TYPE, java.lang.annotation.ElementType.PACKAGE,
				java.lang.annotation.ElementType.TYPE_PARAMETER, java.lang.annotation.ElementType.MODULE,
				java.lang.annotation.ElementType.RECORD_COMPONENT
			})
			public @interface A {
				int value();
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("@A(0) int a;")
	@Test
	void default_annotation_target_incompatible() {
		// Default to all except TYPE_USE
		var v1 = """
			public @interface A {
				int value();
			}""";
		var v2 = """
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.PARAMETER,
				java.lang.annotation.ElementType.CONSTRUCTOR, /* java.lang.annotation.ElementType.LOCAL_VARIABLE, */
				java.lang.annotation.ElementType.ANNOTATION_TYPE, java.lang.annotation.ElementType.PACKAGE,
				java.lang.annotation.ElementType.TYPE_PARAMETER, java.lang.annotation.ElementType.MODULE,
				java.lang.annotation.ElementType.RECORD_COMPONENT
			})
			public @interface A {
				int value();
			}""";

		assertBC("A", "A", BreakingChangeKind.ANNOTATION_TARGET_REMOVED, 1, buildDiff(v1, v2));
	}

	@Client("// No uses")
	@Test
	void empty_annotation_target_compatible() {
		// @Target({}) cannot be used anywhere
		var v1 = """
			@java.lang.annotation.Target({})
			public @interface A {
				String value();
			}""";
		var v2 = """
			@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
			public @interface A {
				String value();
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("@A(0) int a;")
	@Test
	void empty_annotation_target_incompatible() {
		// @Target({}) cannot be used anywhere
		var v1 = """
			@java.lang.annotation.Target(java.lang.annotation.ElementType.LOCAL_VARIABLE)
			public @interface A {
				int value();
			}""";
		var v2 = """
			@java.lang.annotation.Target({})
			public @interface A {
				int value();
			}""";

		assertBC("A", "A", BreakingChangeKind.ANNOTATION_TARGET_REMOVED, 2, buildDiff(v1, v2));
	}
}
