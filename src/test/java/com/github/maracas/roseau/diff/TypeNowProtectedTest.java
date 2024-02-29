package com.github.maracas.roseau.diff;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertBC;
import static com.github.maracas.roseau.TestUtils.assertNoBC;
import static com.github.maracas.roseau.TestUtils.buildDiff;
import static com.github.maracas.roseau.diff.changes.BreakingChangeKind.TYPE_NOW_PROTECTED;
import static com.github.maracas.roseau.diff.changes.BreakingChangeKind.TYPE_REMOVED;

class TypeNowProtectedTest {
	@Test
	void public_nested_type_now_protected() {
		String v1 = "public class A { public class B {} }";
		String v2 = "public class A { protected class B {} }";

		assertBC("A$B", TYPE_NOW_PROTECTED, 1, buildDiff(v1, v2));
	}
}
