package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class MethodNoLongerThrowsCheckedExceptionTest {
	@Test
	void method_no_longer_throws() {
		var v1 = """
			public class A {
				public void m() throws Exception {}
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Test
	void method_no_longer_throws_indirect() {
		var v1 = """
			public class A {
				public void m() throws Exception {}
			}
			public class B extends A {}""";
		var v2 = """
			public class A {
				public void m() {}
			}
			public class B extends A {}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Test
	void method_no_longer_throws_indirect_with_override_without_throws() {
		var v1 = """
			public class A {
				public void m() throws Exception {}
			}
			public class B extends A {
				@Override public void m() {}
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}
			public class B extends A {
				@Override public void m() {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Test
	void method_no_longer_throws_unchecked() {
		var v1 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}""";

		assertNoBC(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, buildDiff(v1, v2));
	}

	@Test
	void method_now_throws_subtype() {
		var v1 = """
			public class A {
				public void m() throws Exception {}
			}""";
		var v2 = """
			public class A {
				public void m() throws java.io.IOException {}
			}""";

		assertNoBC(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, buildDiff(v1, v2));
	}

	@Test
	void method_now_throws_supertype() {
		var v1 = """
			public class A {
				public void m() throws java.io.IOException {}
			}""";
		var v2 = """
			public class A {
				public void m() throws Exception {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Test
	void method_no_longer_throws_type_parameter() {
		var v1 = """
			public class A {
				public <T extends Exception> void m() throws T {}
			}""";
		var v2 = """
			public class A {
				public <T extends Exception> void m() {}
			}""";

		// FIXME
		// assertBC("A.m", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}
}
