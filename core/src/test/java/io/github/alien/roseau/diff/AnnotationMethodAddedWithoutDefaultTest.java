package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class AnnotationMethodAddedWithoutDefaultTest {
	@Client("@A(i=0) int a;")
	@Test
	void new_annotation_method_without_default() {
		var v1 = """
			public @interface A {
				int i();
			}""";
		var v2 = """
			public @interface A {
				int i();
				String s();
			}""";

		assertBC("A", "A", BreakingChangeKind.ANNOTATION_METHOD_ADDED_WITHOUT_DEFAULT, 1, buildDiff(v1, v2));
	}

	@Client("@A(i=0) int a;")
	@Test
	void new_annotation_method_with_default() {
		var v1 = """
			public @interface A {
				int i();
			}""";
		var v2 = """
			public @interface A {
				int i();
				String s() default "";
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
