package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class MethodRemovedTest {
	@Test
	void leaked_public_method_now_private() {
		String v1 = """
			class A {
	public_method_reexposed_from_private_cut			public void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""";
		String v2 = """
			class A {
				void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""";

		assertBC("A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void leaked_public_method_no_longer_leaked() {
		String v1 = """
			class A {
				public void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""";
		String v2 = """
			class A {
				public void m1() {}
			}
			public class B {
				public void m2() {}
			}""";

		assertBC("A.m1", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}
}
