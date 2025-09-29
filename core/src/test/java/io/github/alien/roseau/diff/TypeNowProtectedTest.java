package io.github.alien.roseau.diff;

import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.diff.changes.BreakingChangeKind.CONSTRUCTOR_NOW_PROTECTED;
import static io.github.alien.roseau.diff.changes.BreakingChangeKind.TYPE_NOW_PROTECTED;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class TypeNowProtectedTest {
	@Client("A.B b;")
	@Test
	void public_nested_type_now_protected() {
		var v1 = "public class A { public class B {} }";
		var v2 = "public class A { protected class B {} }";

		assertBCs(buildDiff(v1, v2),
			bc("A$B", "A$B", TYPE_NOW_PROTECTED, 1),
			bc("A$B", "A$B.<init>", CONSTRUCTOR_NOW_PROTECTED, -1));
	}
}
