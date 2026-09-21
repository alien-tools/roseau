package io.github.alien.roseau.diff;

import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adding an overload is usually harmless, but it is source-breaking when the new overload makes an existing call site
 * ambiguous: both candidates are applicable and neither is more specific (JLS §15.12.2.5). Such additions remain
 * binary-compatible, as clients compiled against the old version reference an exact descriptor.
 */
class ExecutableOverloadAddedTest {
	@Disabled("False negative: Roseau reports nothing, but the client below no longer compiles")
	@Client("new A().m(\"a\", \"b\");")
	@Test
	void overload_added_makes_call_ambiguous() {
		var v1 = """
			public class A {
				public void m(Object o, String s) {}
			}""";
		var v2 = """
			public class A {
				public void m(Object o, String s) {}
				public void m(String s, Object o) {}
			}""";

		// javac against v2: "reference to m is ambiguous"
		assertThat(buildDiff(v1, v2)).isNotEmpty();
	}

	@Disabled("False negative: Roseau reports nothing, but the client below no longer compiles")
	@Client("new A(\"a\", \"b\");")
	@Test
	void constructor_overload_added_makes_call_ambiguous() {
		var v1 = """
			public class A {
				public A(Object o, String s) {}
			}""";
		var v2 = """
			public class A {
				public A(Object o, String s) {}
				public A(String s, Object o) {}
			}""";

		assertThat(buildDiff(v1, v2)).isNotEmpty();
	}

	@Client("new A().m(\"a\");")
	@Test
	void overload_added_is_more_specific() {
		var v1 = """
			public class A {
				public void m(Object o) {}
			}""";
		var v2 = """
			public class A {
				public void m(Object o) {}
				public void m(String s) {}
			}""";

		// m(String) is strictly more specific than m(Object), so the call site keeps resolving
		assertNoBC(buildDiff(v1, v2));
	}
}
