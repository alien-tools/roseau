package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class ClassNowAbstractTest {
	@Test
	void class_now_abstract() {
		var v1 = "public class A {}";
		var v2 = "public abstract class A {}";

		assertBC("A", BreakingChangeKind.CLASS_NOW_ABSTRACT, 1, buildDiff(v1, v2));
	}

	@Test
	void interface_now_abstract() {
		var v1 = "public interface I {}";
		var v2 = "public abstract interface I {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void implicitly_abstract_class_now_explicitly_abstract() {
		var v1 = """
			public class A {
				private A() {}
			}""";
		var v2 = """
			public abstract class A {
				protected A() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
