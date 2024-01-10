package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertBC;
import static com.github.maracas.roseau.TestUtils.assertNoBC;
import static com.github.maracas.roseau.TestUtils.buildDiff;

class JezekTest {
	@Test
	void genericsWildcardsClazzConstructorParamLowerBoundsAdd() {
		String v1 = """
			public class A {
			  public A(List<?> l) {}
			}""";
		String v2 = """
			public class A {
			  public A(List<A> l) {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.CONSTRUCTOR_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void circular() {
		String v1 = """
			public class A {
			  public <T extends Number & Comparable<T>> void m() {}
			}""";
		String v2 = """
			public class A {
			  public <T extends Number & List<T>> void m() {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void dataTypeIfazeConstantNarrowing() {
		String v1 = """
			public class A {
			  public double f;
			}""";
		String v2 = """
			public class A {
			  public int f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void exceptionClazzMethodThrowUncheckedAdd() {
		String v1 = """
			public class A {
				public void m() {}
			}""";
		String v2 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void exceptionClazzMethodThrowUncheckedDelete() {
		String v1 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";
		String v2 = """
			public class A {
				public void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void exceptionClazzMethodThrowUncheckedGeneralization() {
		String v1 = """
			public class A {
				public void m() throws IllegalArgumentException {}
			}""";
		String v2 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void exceptionClazzMethodThrowUncheckedSpecialization() {
		String v1 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";
		String v2 = """
			public class A {
				public void m() throws IllegalArgumentException {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
