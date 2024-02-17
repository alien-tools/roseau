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

	@Test
	void exceptionClazzMethodThrowUncheckedMutation() {
		String v1 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";
		String v2 = """
			public class A {
				public void m() throws Exception {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Test
	void modifierClazzEffectivelyFinalToFinal() {
		String v1 = """
			public class A {
				private A() {}
			}""";
		String v2 = """
			public final class A {
				private A() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void modifierClazzFinalToEffectivelyFinal() {
		String v1 = """
			public final class A {
				private A() {}
			}""";
		String v2 = """
			public class A {
				private A() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void inheritanceIfazeMethodMovedFromSuperInterface() {
		String v1 = """
			public interface S {
				void m();
			}
			public interface I extends S {}""";
		String v2 = """
			public interface S {}
			public interface I extends S {
				void m();
			}""";

		var bcs = buildDiff(v1, v2);
		assertBC("S.m", BreakingChangeKind.METHOD_REMOVED, 2, bcs);
		assertNoBC(4, bcs);
	}

	@Test
	void inheritanceIfazeMethodMovedToSuperInterface() {
		String v1 = """
			public interface S {}
			public interface I extends S {
				void m();
			}""";
		String v2 = """
			public interface S {
				void m();
			}
			public interface I extends S {}""";

		var bcs = buildDiff(v1, v2);
		assertBC("S", BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, 1, bcs);
		assertNoBC(3, bcs);
	}

	@Test
	void inheritanceFieldMovedFromSuperClass() {
		String v1 = """
			public class S {
				public int f;
			}
			public class C extends S {}""";
		String v2 = """
			public class S {}
			public class C extends S {
				public int f;
			}""";

		var bcs = buildDiff(v1, v2);
		assertBC("S.f", BreakingChangeKind.FIELD_REMOVED, 2, bcs);
		assertNoBC(4, bcs);
	}

	@Test
	void inheritanceFieldMovedToSuperClass() {
		String v1 = """
			public class S {}
			public class C extends S {
				public int f;
			}""";
		String v2 = """
			public class S {
				public int f;
			}
			public class C extends S {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzConstructorTypeAddN() {
		String v1 = """
			public class C {
				public C() {}
			}""";
		String v2 = """
			public class C {
				public <T> C() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzMethodTypeAddN() {
		String v1 = """
			public class C {
				public void m() {}
			}""";
		String v2 = """
			public class C {
				public <T> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzTypeAddN() {
		String v1 = "public class C {}";
		String v2 = "public class C<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsIfazeMethodTypeAddN() {
		String v1 = """
			public interface I {
				public void m();
			}""";
		String v2 = """
			public interface I {
				public <T> void m();
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsIfazeTypeAddN() {
		String v1 = "public interface I {}";
		String v2 = "public interface I<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsWildcardsClazzConstructorParamLowerBoundsAdd2() {
		String v1 = """
			public class C {
				public void m(ArrayList<?> al) {}
			}""";
		String v2 = """
			public class C {
				public void m(ArrayList<? extends Number> al) {}
			}""";

		assertBC("C.m", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void inheritanceIfazeStartInherite() {
		String v1 = """
			public interface I {}""";
		String v2 = """
			public interface J {
				void m();
			}
			public interface I extends J {}""";

		assertBC("I", BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, 1, buildDiff(v1, v2));
	}
}
