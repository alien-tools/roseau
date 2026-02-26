package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

/**
 * Should check for overlaps and merge them with "regular" tests
 */
class JezekTest {
	@Test
	void genericsWildcardsClazzConstructorParamLowerBoundsAdd() {
		var v1 = """
			public class A {
			  public A(java.util.List<?> l) {}
			}""";
		var v2 = """
			public class A {
			  public A(java.util.List<A> l) {}
			}""";

		assertBC("A", "A.<init>(java.util.List<? extends java.lang.Object>)", BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void circular() {
		var v1 = """
			public class A {
			  public <T extends Number & Comparable<T>> void m() {}
			}""";
		var v2 = """
			public class A {
			  public <T extends Number & java.util.List<T>> void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void dataTypeClazzFieldNarrowing() {
		var v1 = """
			public class A {
			  public double f;
			}""";
		var v2 = """
			public class A {
			  public int f;
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.f", BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.f", BreakingChangeKind.FIELD_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Test
	void dataTypeIfazeConstantNarrowing() {
		var v1 = """
			public interface I {
			  public double f = 5;
			}""";
		var v2 = """
			public interface I {
			  public int f = 5;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void exceptionClazzMethodThrowUncheckedAdd() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void exceptionClazzMethodThrowUncheckedDelete() {
		var v1 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void exceptionClazzMethodThrowUncheckedGeneralization() {
		var v1 = """
			public class A {
				public void m() throws IllegalArgumentException {}
			}""";
		var v2 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void exceptionClazzMethodThrowUncheckedSpecialization() {
		var v1 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";
		var v2 = """
			public class A {
				public void m() throws IllegalArgumentException {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void exceptionClazzMethodThrowUncheckedMutation() {
		var v1 = """
			public class A {
				public void m() throws RuntimeException {}
			}""";
		var v2 = """
			public class A {
				public void m() throws Exception {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Test
	void exceptionClazzMethodThrowCheckedSpecialization() {
		var v1 = """
			public class A {
				public void m() throws java.io.IOException {}
			}""";
		var v2 = """
			public class A {
				public void m() throws java.io.FileNotFoundException {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2, buildDiff(v1, v2));
	}

	@Test
	void modifierClazzEffectivelyFinalToFinal() {
		var v1 = """
			public class A {
				private A() {}
			}""";
		var v2 = """
			public final class A {
				private A() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void modifierClazzFinalToEffectivelyFinal() {
		var v1 = """
			public final class A {
				private A() {}
			}""";
		var v2 = """
			public class A {
				private A() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void inheritanceIfazeMethodMovedFromSuperInterface() {
		var v1 = """
			public interface S {
				void m();
			}
			public interface I extends S {}""";
		var v2 = """
			public interface S {}
			public interface I extends S {
				void m();
			}""";

		assertBC("S", "S.m()", BreakingChangeKind.METHOD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void inheritanceIfazeMethodMovedToSuperInterface() {
		var v1 = """
			public interface S {}
			public interface I extends S {
				void m();
			}""";
		var v2 = """
			public interface S {
				void m();
			}
			public interface I extends S {}""";

		assertBC("S", "S", BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, 1, buildDiff(v1, v2));
	}

	@Test
	void inheritanceFieldMovedFromSuperClass() {
		var v1 = """
			public class S {
				public int f;
			}
			public class C extends S {}""";
		var v2 = """
			public class S {}
			public class C extends S {
				public int f;
			}""";

		assertBC("S", "S.f", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void inheritanceFieldMovedToSuperClass() {
		var v1 = """
			public class S {}
			public class C extends S {
				public int f;
			}""";
		var v2 = """
			public class S {
				public int f;
			}
			public class C extends S {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzConstructorTypeAddN() {
		var v1 = """
			public class C {
				public C() {}
			}""";
		var v2 = """
			public class C {
				public <T> C() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzMethodTypeAddN() {
		var v1 = """
			public class C {
				public void m() {}
			}""";
		var v2 = """
			public class C {
				public <T> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzTypeAddN() {
		var v1 = "public class C {}";
		var v2 = "public class C<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsIfazeMethodTypeAddN() {
		var v1 = """
			public interface I {
				public void m();
			}""";
		var v2 = """
			public interface I {
				public <T> void m();
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsIfazeTypeAddN() {
		var v1 = "public interface I {}";
		var v2 = "public interface I<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsWildcardsClazzConstructorParamLowerBoundsAdd2() {
		var v1 = """
			public class C {
				public void m(java.util.ArrayList<?> al) {}
			}""";
		var v2 = """
			public class C {
				public void m(java.util.ArrayList<? extends Number> al) {}
			}""";

		assertBC("C", "C.m(java.util.ArrayList<? extends java.lang.Object>)", BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void inheritanceIfazeStartInherite() {
		var v1 = """
			public interface I {}""";
		var v2 = """
			public interface J {
				void m();
			}
			public interface I extends J {}""";

		assertBC("I", "I", BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, 1, buildDiff(v1, v2));
	}

	@Test
	void genericsIfazeTypeBoundsAdd() {
		var v1 = "public interface I<T> {}";
		var v2 = "public interface I<T extends Number> {}";

		assertBC("I", "I", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void genericsIfazeTypeBoundsDeleteN() {
		var v1 = "public interface I<T extends Number> {}";
		var v2 = "public interface I<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzConstructorTypeBoundsDeleteN() {
		var v1 = """
			public class C {
				public <T extends Number> C() {}
			}""";
		var v2 = """
			public class C {
				public <T> C() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzConstructorTypeBoundsDeleteSecond() {
		var v1 = """
			public class C {
				public <T extends Number & Comparable<T>> C() {}
			}""";
		var v2 = """
			public class C {
				public <T extends Number> C() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzConstructorTypeBoundsGeneralization() {
		var v1 = """
			public class C {
				public <T extends Integer> C() {}
			}""";
		var v2 = """
			public class C {
				public <T extends Number> C() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsIfazeTypeBoundsGeneralization() {
		var v1 = "public interface I<T extends Integer> {}";
		var v2 = "public interface I<T extends Number> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsWildcardsClazzMethodParamAdd() {
		var v1 = """
			public class C {
				public void m(java.util.List<String> l) {}
			}""";
		var v2 = """
			public class C {
				public void m(java.util.List<?> l) {}
			}""";

		assertBC("C", "C.m(java.util.List<java.lang.String>)", BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void genericsWildcardsClazzConstructorParamLowerBoundsSpecialization() {
		var v1 = """
			public class C {
				public C(java.util.List<? super Number> l) {}
			}""";
		var v2 = """
			public class C {
				public C(java.util.List<? super Integer> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzConstructorTypeDeleteN() {
		var v1 = """
			public class C {
				public <T> C() {}
			}""";
		var v2 = """
			public class C {
				public C() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void genericsClazzMethodTypeDeleteN() {
		var v1 = """
			public class C {
				public <T> void m() {}
			}""";
		var v2 = """
			public class C {
				public void m() {}
			}""";

		assertBC("C", "C.m()", BreakingChangeKind.FORMAL_TYPE_PARAMETER_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void genericsWildcardsClazzConstructorParamAdd() {
		var v1 = """
			public class C {
				public C(java.util.ArrayList<Integer> l) {}
			}""";
		var v2 = """
			public class C {
				public C(java.util.ArrayList<?> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void membersIfazeNestedIfazeDelete() {
		var v1 = """
			public interface I1 {
				public interface I2 {}
			}""";
		var v2 = "public interface I1 {}";

		assertBC("I1$I2", "I1$I2", BreakingChangeKind.TYPE_REMOVED, 2, buildDiff(v1, v2));
	}
}
