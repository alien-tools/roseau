package io.github.alien.roseau.diff;

import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.diff.changes.BreakingChangeKind.TYPE_NOW_PROTECTED;
import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class TypeNowProtectedTest {
	@Client("A.B b;")
	@Test
	void public_nested_type_now_protected() {
		var v1 = "public class A { public class B {} }";
		var v2 = "public class A { protected class B {} }";

		assertBC("A$B", TYPE_NOW_PROTECTED, 1, buildDiff(v1, v2));
	}
}
