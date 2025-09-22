package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class AnnotationTargetRemovedTest {
	@Test
	void annotation_target_changed() {
		var v1 = """
			@java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
			public @interface A {
				String value();
			}""";
		var v2 = """
			@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
			public @interface A {
				String value();
			}""";

		assertBC("A", BreakingChangeKind.ANNOTATION_TARGET_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void annotation_target_removed() {
		var v1 = """
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.METHOD,
				java.lang.annotation.ElementType.TYPE
			})
			public @interface A {
				String value();
			}""";
		var v2 = """
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.TYPE
			})
			public @interface A {
				String value();
			}""";

		assertBC("A", BreakingChangeKind.ANNOTATION_TARGET_REMOVED, 6, buildDiff(v1, v2));
	}

	@Test
	void annotation_target_added() {
		var v1 = """
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.TYPE
			})
			public @interface A {
				String value();
			}""";
		var v2 = """
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.METHOD,
				java.lang.annotation.ElementType.TYPE
			})
			public @interface A {
				String value();
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
