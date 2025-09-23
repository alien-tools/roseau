package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class AnnotationMethodAddedWithoutDefaultTest {
	@Test
	void new_annotation_method_without_default() {
		var v1 = """
			public @interface A {
				String s();
			}""";
		var v2 = """
			public @interface A {
				String s();
				int i();
			}""";

		assertBC("A", BreakingChangeKind.ANNOTATION_METHOD_ADDED_WITHOUT_DEFAULT, 1, buildDiff(v1, v2));
	}

	@Test
	void new_annotation_method_with_default() {
		var v1 = """
			public @interface A {
				String s();
			}""";
		var v2 = """
			public @interface A {
				String s();
				int i() default 0;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
