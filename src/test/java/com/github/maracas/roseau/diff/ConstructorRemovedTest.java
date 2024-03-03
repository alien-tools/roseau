package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class ConstructorRemovedTest {
	@Test
	void class_default_constructor_removed() {
		String v1 = "public class A {}";
		String v2 = """
			public class A {
				public A(int i) {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1, buildDiff(v1, v2));
	}

	@Test
	void class_default_constructor_now_explicit() {
		String v1 = "public class A {}";
		String v2 = """
			public class A {
				public A() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_explicit_constructor_now_default() {
		String v1 = """
			public class A {
				public A() {}
			}""";
		String v2 = "public class A {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_constructor_now_private() {
		String v1 = "public class A {}";
		String v2 = """
			public class A {
				private A() {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1, buildDiff(v1, v2));
	}

	@Test
	void class_constructor_now_protected() {
		String v1 = "public class A {}";
		String v2 = """
			public class A {
				protected A() {}
			}""";

		assertNoBC(BreakingChangeKind.CONSTRUCTOR_REMOVED, buildDiff(v1, v2));
	}

	@Test
	void record_constructor_changed() {
		String v1 = "public record A(int i) {}";
		String v2 = "public record A(float f) {}";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, -1, buildDiff(v1, v2));
	}

	@Test
	void class_constructor_changed() {
		String v1 = """
			public class A {
				public A(int i) {}
			}""";
		String v2 = """
			public class A {
				public A(float f) {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void overloaded_constructor_removed() {
		String v1 = """
			public class A {
				public A(int i) {}
				public A(float f) {}
			}""";
		String v2 = """
			public class A {
				public A(int i) {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, 3, buildDiff(v1, v2));
	}
}
