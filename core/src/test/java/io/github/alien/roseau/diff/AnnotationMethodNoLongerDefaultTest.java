package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class AnnotationMethodNoLongerDefaultTest {
	@Test
	void annotation_method_no_longer_default() {
		var v1 = """
			public @interface A {
				String value() default "";
			}""";
		var v2 = """
			public @interface A {
				String value();
			}""";

		assertBC("A.value", BreakingChangeKind.ANNOTATION_METHOD_NO_LONGER_DEFAULT, 2, buildDiff(v1, v2));
	}

	@Test
	void annotation_method_now_default() {
		var v1 = """
			public @interface A {
				String value();
			}""";
		var v2 = """
			public @interface A {
				String value() default "";
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
