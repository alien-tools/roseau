package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class MethodNoLongerVarargsTest {
	@Test
	void method_no_longer_varargs_first() {
		var v1 = """
			public class A {
				public void m(int... i) {}
			}""";
		var v2 = """
			public class A {
				public void m(int i) {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NO_LONGER_VARARGS, 2, buildDiff(v1, v2));
	}

	@Test
	void method_no_longer_varargs_last() {
		var v1 = """
			public class A {
				public void m(String s, int... i) {}
			}""";
		var v2 = """
			public class A {
				public void m(String s, int i) {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NO_LONGER_VARARGS, 2, buildDiff(v1, v2));
	}
}
