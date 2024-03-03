package com.github.maracas.roseau.onthefly;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.OnTheFlyCaseCompiler.assertBC;
import static com.github.maracas.roseau.utils.OnTheFlyCaseCompiler.assertNoBC;

class OnTheFlyClassNowAbstractTest {
	@Test
	void class_now_abstract() {
		assertBC(
			"public class A {}",
			"public abstract class A {}",
			"void main() { new A(); }",
			"A", BreakingChangeKind.CLASS_NOW_ABSTRACT, 1
		);
	}

	@Test
	void interface_now_abstract() {
		assertNoBC(
			"public class A {}",
			"public class A {}",
			"void main() { new A(); }"
		);
	}
}
