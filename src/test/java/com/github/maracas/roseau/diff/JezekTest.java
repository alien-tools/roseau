package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class JezekTest {
	@Test
	void genericsWildcardsClazzConstructorParamLowerBoundsAdd() {
		String v1 = """
			public class A {
			  public A(java.util.List<?> l) {}
			}""";
		String v2 = """
			public class A {
			  public A(java.util.List<A> l) {}
			}""";

		assertBC("A.<init>", BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void circular() {
		String v1 = """
			public class A {
			  public <T extends Number & Comparable<T>> void m() {}
			}""";
		String v2 = """
			public class A {
			  public <T extends Number & java.util.List<T>> void m() {}
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void dataTypeClazzFieldNarrowing() {
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
	void dataTypeIfazeConstantNarrowing() {
		String v1 = """
			public interface I {
			  public double f = 5;
			}""";
		String v2 = """
			public interface I {
			  public int f = 5;
			}""";

		assertBC("I.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
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
	void exceptionClazzMethodThrowCheckedSpecialization() {
		String v1 = """
			public class A {
				public void m() throws java.io.IOException {}
			}""";
		String v2 = """
			public class A {
				public void m() throws java.io.FileNotFoundException {}
			}""";

		assertNoBC(buildDiff(v1, v2));
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
				public void m(java.util.ArrayList<?> al) {}
			}""";
		String v2 = """
			public class C {
				public void m(java.util.ArrayList<? extends Number> al) {}
			}""";

		assertBC("C.m", BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
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

	@Test
	void genericsIfazeTypeBoundsAdd() {
		String v1 = "public interface I<T> {}";
		String v2 = "public interface I<T extends Number> {}";

		assertBC("I", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void genericsIfazeTypeBoundsDeleteN() {
		String v1 = "public interface I<T extends Number> {}";
		String v2 = "public interface I<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzConstructorTypeBoundsDeleteN() {
		String v1 = """
			public class C {
				public <T extends Number> C() {}
			}""";
		String v2 = """
			public class C {
				public <T> C() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzConstructorTypeBoundsDeleteSecond() {
		String v1 = """
			public class C {
				public <T extends Number & Comparable<T>> C() {}
			}""";
		String v2 = """
			public class C {
				public <T extends Number> C() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzConstructorTypeBoundsGeneralization() {
		String v1 = """
			public class C {
				public <T extends Integer> C() {}
			}""";
		String v2 = """
			public class C {
				public <T extends Number> C() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsIfazeTypeBoundsGeneralization() {
		String v1 = "public interface I<T extends Integer> {}";
		String v2 = "public interface I<T extends Number> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsWildcardsClazzMethodParamAdd() {
		String v1 = """
			public class C {
				public void m(java.util.List<String> l) {}
			}""";
		String v2 = """
			public class C {
				public void m(java.util.List<?> l) {}
			}""";

		assertBC("C.m", BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void genericsWildcardsClazzConstructorParamLowerBoundsSpecialization() {
		String v1 = """
			public class C {
				public C(java.util.List<? super Number> l) {}
			}""";
		String v2 = """
			public class C {
				public C(java.util.List<? super Integer> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzConstructorTypeDeleteN() {
		String v1 = """
			public class C {
				public <T> C() {}
			}""";
		String v2 = """
			public class C {
				public C() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzMethodTypeDeleteN() {
		String v1 = """
			public class C {
				public <T> void m() {}
			}""";
		String v2 = """
			public class C {
				public void m() {}
			}""";

		assertBC("C.m", BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void genericsWildcardsClazzConstructorParamAdd() {
		String v1 = """
			public class C {
				public C(java.util.ArrayList<Integer> l) {}
			}""";
		String v2 = """
			public class C {
				public C(java.util.ArrayList<?> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
