package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class AnnotationNoLongerRepeatableTest {
	@Test
	void annotation_no_longer_repeatable() {
		var v1 = """
			@java.lang.annotation.Repeatable(Container.class)
			public @interface A {}
			public @interface Container {
				A[] value();
			}""";
		var v2 = """
			public @interface A {}
			public @interface Container {
				A[] value();
			}""";

		assertBC("A", BreakingChangeKind.ANNOTATION_NO_LONGER_REPEATABLE, 2, buildDiff(v1, v2));
	}
}
