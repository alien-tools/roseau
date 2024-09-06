package com.github.maracas.roseau.diff;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static com.github.maracas.roseau.utils.TestUtils.*;
import static com.github.maracas.roseau.diff.changes.BreakingChangeKind.METHOD_REMOVED;
import static com.github.maracas.roseau.diff.changes.BreakingChangeKind.TYPE_REMOVED;

class IgnoreTest {
	@Test
	void class_ignore() {
		String v1 = "public class FooTest {}";
		String v2 = "public class BarTest {}";
		List<Pattern> ignorePatterns = List.of(Pattern.compile(".*Test.*"));
		assertBC("FooTest", TYPE_REMOVED, 1, buildDiff(v1, v2));
		assertNoBC(buildDiff(v1, v2, ignorePatterns));
	}

	@Test
	void member_ignore() {
		String v1 = "public class Test { public void foo() { } }";
		String v2 = "public class Test { public void bar() { } }";
		List<Pattern> ignorePatterns = List.of(Pattern.compile(".*Test.*"));
		assertBC("Test.foo", METHOD_REMOVED, 1, buildDiff(v1, v2));
		assertNoBC(buildDiff(v1, v2, ignorePatterns));
	}
}
