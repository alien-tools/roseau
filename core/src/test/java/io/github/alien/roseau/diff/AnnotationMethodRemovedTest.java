package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class AnnotationMethodRemovedTest {
	@Test
	void annotation_method_removed() {
		var v1 = """
			public @interface A {
				String value();
			}""";
		var v2 = "public @interface A {}";

		assertBC("A.value", BreakingChangeKind.ANNOTATION_METHOD_REMOVED, 2, buildDiff(v1, v2));
	}
}
