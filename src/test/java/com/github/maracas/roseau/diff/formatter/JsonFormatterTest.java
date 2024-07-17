package com.github.maracas.roseau.diff.formatter;

import com.github.maracas.roseau.diff.changes.BreakingChange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.maracas.roseau.utils.TestUtils.buildDiff;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class JsonFormatterTest {
	@Test
	void method_removed_in_JSON_format() {
		var v1 = """
			class A {
				public void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""";
		var v2 = """
			class A {
				void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""";

		List<BreakingChange> breakingChanges = buildDiff(v1, v2);
		JsonFormatter formatter = new JsonFormatter();
		var result = formatter.format(breakingChanges);
		String expectedResult = """
			[{"nature":"DELETION","oldLocation":{"path":"A.java","line":2},"kind":"METHOD_REMOVED","element":"A.m1"}]""";

		assertThat(result, is(equalTo(expectedResult)));
	}

	@Test
	void list_of_breaking_changes_in_JSON_format() {
		var v1 = """
			public class A {}
			public class B {}""";

		var v2 = """
			public abstract class A {}
			public final class B {}""";

		List<BreakingChange> breakingChanges = buildDiff(v1, v2);
		JsonFormatter formatter = new JsonFormatter();
		var result = formatter.format(breakingChanges);
		String expectedResult = """
			[{"nature":"MUTATION","oldLocation":{"path":"A.java","line":2},"kind":"CLASS_NOW_FINAL","newLocation":{"path":"A.java","line":2},"element":"B"},{"nature":"MUTATION","oldLocation":{"path":"A.java","line":1},"kind":"CLASS_NOW_ABSTRACT","newLocation":{"path":"A.java","line":1},"element":"A"}]""";

		assertThat(result, is(equalTo(expectedResult)));
	}
}
