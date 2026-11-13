package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class ExecutableParameterGenericsChangedTest {
	@Client("new A().m(java.util.List.<String>of());")
	@Test
	void different_generic_arguments() {
		var v1 = """
			public class A {
				public void m(java.util.List<String> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<Integer> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<java.lang.String>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<String>of());")
	@Test
	void same_generic_argument() {
		var v1 = """
			public class A {
				public void m(java.util.List<String> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<String> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<String>of(), java.util.List.<String>of());")
	@Test
	void second_parameter_changed() {
		var v1 = """
			public class A {
				public void m(java.util.List<String> l1, java.util.List<String> l2) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<String> l1, java.util.List<Integer> l2) {}
			}""";

		assertBC("A", "A.m(java.util.List<java.lang.String>,java.util.List<java.lang.String>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<Number>of());")
	@Test
	void upper_bounded_wildcard_narrowed() {
		var v1 = """
			public class A {
				public void m(java.util.List<? extends Number> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<? extends Integer> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<? extends java.lang.Number>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.Map.<String, java.util.List<String>>of());")
	@Test
	void nested_generic_changed() {
		var v1 = """
			public class A {
				public void m(java.util.Map<String, java.util.List<String>> map) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.Map<String, java.util.List<Integer>> map) {}
			}""";

		assertBC("A", "A.m(java.util.Map<java.lang.String,java.util.List<java.lang.String>>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	// §4.5.1: T <= ? (via T <= ? extends T and ? extends T <= ?), so List<String> <: List<?>
	// but overriding breaks
	@Client("""
		new A().m(java.util.List.<String>of());
		new A() {
			@Override public void m(java.util.List<String> l) {}
		}.m(java.util.List.<String>of());""")
	@Test
	void concrete_to_unbounded_wildcard() {
		var v1 = """
			public class A {
				public void m(java.util.List<String> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<?> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<java.lang.String>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<String>of());")
	@Test
	void concrete_to_unbounded_wildcard_final() {
		var v1 = """
			public class A {
				public final void m(java.util.List<String> l) {}
			}""";
		var v2 = """
			public class A {
				public final void m(java.util.List<?> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<Integer>of());")
	@Test
	void unbounded_to_concrete_wildcard() {
		var v1 = """
			public class A {
				public void m(java.util.List<?> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<String> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<? extends java.lang.Object>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	// §4.8, §5.1.9, §8.4.2: every argument accepted by a raw List is accepted by List<?>, and the raw m(List) is
	// still a subsignature of m(List<?>), so overriders and hiders are unaffected
	@Client("""
		new A().m(new java.util.ArrayList());
		new A().m(java.util.List.<String>of());
		new A() {
			@Override public void m(java.util.List l) {}
		}.m(java.util.List.<String>of());""")
	@Test
	void raw_to_unbounded_wildcard() {
		var v1 = """
			public class A {
				public void m(java.util.List l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<?> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<String>of());")
	@Test
	void raw_to_explicit_unbounded_wildcard() {
		var v1 = """
			public class A {
				public void m(java.util.List l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<? extends Object> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		A.m(String.class);
		class B extends A {
			public static void m(Class c) {}
		}""")
	@Test
	void raw_to_unbounded_wildcard_static() {
		var v1 = """
			public class A {
				public static void m(Class c) {}
			}""";
		var v2 = """
			public class A {
				public static void m(Class<?> c) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A(java.util.List.<String>of());")
	@Test
	void raw_to_unbounded_wildcard_constructor() {
		var v1 = """
			public class A {
				public A(java.util.List l) {}
			}""";
		var v2 = """
			public class A {
				public A(java.util.List<?> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.Map.<String, Integer>of());")
	@Test
	void raw_to_unbounded_wildcards() {
		var v1 = """
			public class A {
				public void m(java.util.Map m) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.Map<?, ?> m) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.Map.<Integer, Integer>of());")
	@Test
	void raw_to_partially_unbounded_wildcards() {
		var v1 = """
			public class A {
				public void m(java.util.Map m) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.Map<String, ?> m) {}
			}""";

		assertBC("A", "A.m(java.util.Map)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<String>of());")
	@Test
	void raw_to_bounded_wildcard() {
		var v1 = """
			public class A {
				public void m(java.util.List l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<? extends Number> l) {}
			}""";

		assertBC("A", "A.m(java.util.List)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<Integer>of());")
	@Test
	void raw_to_concrete() {
		var v1 = """
			public class A {
				public void m(java.util.List l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<String> l) {}
			}""";

		assertBC("A", "A.m(java.util.List)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	// §8.4.2: m(List, List<String>) is neither m(List<?>, List<String>) nor its erasure m(List, List)
	@Client("""
		new A() {
			@Override public void m(java.util.List l1, java.util.List<String> l2) {}
		};""")
	@Test
	void raw_to_unbounded_wildcard_next_to_parameterized_parameter() {
		var v1 = """
			public class A {
				public void m(java.util.List l1, java.util.List<String> l2) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<?> l1, java.util.List<String> l2) {}
			}""";

		assertBC("A", "A.m(java.util.List,java.util.List<java.lang.String>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<Integer>of(), java.util.List.<String>of());")
	@Test
	void raw_to_unbounded_wildcard_next_to_parameterized_parameter_final() {
		var v1 = """
			public class A {
				public final void m(java.util.List l1, java.util.List<String> l2) {}
			}""";
		var v2 = """
			public class A {
				public final void m(java.util.List<?> l1, java.util.List<String> l2) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §8.4.2: m(List, T) is neither m(List<?>, T) nor its erasure m(List, Object)
	@Client("""
		new A<String>() {
			@Override public void m(java.util.List l, String t) {}
		};""")
	@Test
	void raw_to_unbounded_wildcard_next_to_type_variable_parameter() {
		var v1 = """
			public class A<T> {
				public void m(java.util.List l, T t) {}
			}""";
		var v2 = """
			public class A<T> {
				public void m(java.util.List<?> l, T t) {}
			}""";

		assertBC("A", "A.m(java.util.List,T)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		class B extends A {
			public static void m(Class c, java.util.List<String> l) {}
		}""")
	@Test
	void raw_to_unbounded_wildcard_next_to_parameterized_parameter_static() {
		var v1 = """
			public class A {
				public static void m(Class c, java.util.List<String> l) {}
			}""";
		var v2 = """
			public class A {
				public static void m(Class<?> c, java.util.List<String> l) {}
			}""";

		assertBC("A", "A.m(java.lang.Class,java.util.List<java.lang.String>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	// m(List, String, List[]) is the erasure of m(List<?>, String, List<?>[])
	@Client("""
		new A() {
			@Override public void m(java.util.List l, String s, java.util.List[] ls) {}
		}.m(java.util.List.<Integer>of(), "", new java.util.List[0]);""")
	@Test
	void raw_to_unbounded_wildcard_next_to_erased_parameters() {
		var v1 = """
			public class A {
				public void m(java.util.List l, String s, java.util.List[] ls) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<?> l, String s, java.util.List<?>[] ls) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §8.4.2: m(List<?>) is not a subsignature of the raw m(List), so overriders break
	@Client("""
		new A() {
			@Override public void m(java.util.List<?> l) {}
		}.m(java.util.List.<String>of());""")
	@Test
	void unbounded_wildcard_to_raw() {
		var v1 = """
			public class A {
				public void m(java.util.List<?> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List l) {}
			}""";

		assertBC("A", "A.m(java.util.List<? extends java.lang.Object>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	// §4.8: every parameterization of List is a subtype of the raw List
	@Client("new A().m(java.util.List.<String>of());")
	@Test
	void unbounded_wildcard_to_raw_final() {
		var v1 = """
			public class A {
				public final void m(java.util.List<?> l) {}
			}""";
		var v2 = """
			public class A {
				public final void m(java.util.List l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §8.4.2: m(List<String>) is not a subsignature of the raw m(List), so overriders break
	@Client("""
		new A() {
			@Override public void m(java.util.List<String> l) {}
		}.m(java.util.List.<String>of());""")
	@Test
	void concrete_to_raw() {
		var v1 = """
			public class A {
				public void m(java.util.List<String> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List l) {}
			}""";

		assertBC("A", "A.m(java.util.List<java.lang.String>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A(java.util.List.<String>of());")
	@Test
	void concrete_to_raw_constructor() {
		var v1 = """
			public class A {
				public A(java.util.List<String> l) {}
			}""";
		var v2 = """
			public class A {
				public A(java.util.List l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §4.5.1: T <= ? super T, so List<Integer> <: List<? super Integer>
	// but breaks overriders
	@Client("""
		new A().m(java.util.List.<Integer>of());
		new A() {
			@Override public void m(java.util.List<Integer> l) {}
		}.m(java.util.List.<Integer>of());""")
	@Test
	void concrete_to_lower_bounded_wildcard() {
		var v1 = """
			public class A {
				public void m(java.util.List<Integer> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<? super Integer> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<java.lang.Integer>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<Integer>of());")
	@Test
	void concrete_to_lower_bounded_wildcard_final() {
		var v1 = """
			public final class A {
				public void m(java.util.List<Integer> l) {}
			}""";
		var v2 = """
			public final class A {
				public void m(java.util.List<? super Integer> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §4.5.1: no rule for ? extends T <= T; List<? extends Integer> is not a subtype of List<Integer>
	@Client("new A().m(java.util.List.<Integer>of());")
	@Test
	void upper_bounded_wildcard_to_concrete() {
		var v1 = """
			public class A {
				public void m(java.util.List<? extends Number> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<Number> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<? extends java.lang.Number>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	// §4.5.1: no rule for ? super T <= T; List<Number> satisfies List<? super Integer> but not List<Integer>
	@Client("new A().m(new java.util.ArrayList<Number>());")
	@Test
	void lower_bounded_wildcard_to_concrete() {
		var v1 = """
			public class A {
				public void m(java.util.List<? super Integer> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<Integer> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<? super java.lang.Integer>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	// §4.5.1 rule 3: ? super T <= ? super S iff S <: T; ? super Integer <= ? super Number requires
	// Number <: Integer which is false — List<Integer> satisfies ? super Integer but not ? super Number
	@Client("new A().m(java.util.List.<Integer>of());")
	@Test
	void lower_bounded_wildcard_widened() {
		var v1 = """
			public class A {
				public void m(java.util.List<? super Integer> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<? super Number> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<? super java.lang.Integer>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	// §4.5.1 rule 1: ? extends T <= ? extends S iff T <: S; Integer <: Number, so
	// List<? extends Integer> <: List<? extends Number> — v2 is wider, all v1 call sites remain valid
	// but breaks implementers
	@Client("""
		new A().m(java.util.List.<Integer>of());
		new A() {
			@Override public void m(java.util.List<? extends Integer> l) {}
		}.m(java.util.List.<Integer>of());""")
	@Test
	void upper_bounded_wildcard_widened() {
		var v1 = """
			public class A {
				public void m(java.util.List<? extends Integer> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<? extends Number> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<? extends java.lang.Integer>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<Integer>of());")
	@Test
	void upper_bounded_wildcard_widened_final() {
		var v1 = """
			public class A {
				public final void m(java.util.List<? extends Integer> l) {}
			}""";
		var v2 = """
			public class A {
				public final void m(java.util.List<? extends Number> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §4.5.1 rule 7: T <= ? extends T, so List<Integer> <: List<? extends Integer>
	// but breaks implementers
	@Client("""
		new A().m(java.util.List.<Integer>of());
		new A() {
			@Override public void m(java.util.List<Integer> l) {}
		}.m(java.util.List.<Integer>of());""")
	@Test
	void concrete_to_upper_bounded_wildcard() {
		var v1 = """
			public class A {
				public void m(java.util.List<Integer> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<? extends Integer> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<java.lang.Integer>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<Integer>of());")
	@Test
	void concrete_to_upper_bounded_wildcard_final() {
		var v1 = """
			public final class A {
				public void m(java.util.List<Integer> l) {}
			}""";
		var v2 = """
			public final class A {
				public void m(java.util.List<? extends Integer> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §4.5.1 rule 3: ? super T <= ? super S iff S <: T; Integer <: Number, so
	// List<? super Number> <: List<? super Integer> — v2 is wider, all v1 call sites remain valid
	// but breaks implementers
	@Client("""
		new A().m(new java.util.ArrayList<Number>());
		new A() {
			@Override public void m(java.util.List<? super Number> l) {}
		}.m(java.util.List.<Number>of());""")
	@Test
	void lower_bounded_wildcard_narrowed() {
		var v1 = """
			public class A {
				public void m(java.util.List<? super Number> l) {}
			}""";
		var v2 = """
			public class A {
				public void m(java.util.List<? super Integer> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<? super java.lang.Number>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(new java.util.ArrayList<Number>());")
	@Test
	void lower_bounded_wildcard_narrowed_final() {
		var v1 = """
			public class A {
				public final void m(java.util.List<? super Number> l) {}
			}""";
		var v2 = """
			public class A {
				public final void m(java.util.List<? super Integer> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().<Number>m(java.util.List.<Integer>of());")
	@Test
	void generic_method_wildcard_removed() {
		var v1 = """
			public class A {
				public <T> void m(java.util.List<? extends T> l) {}
			}""";
		var v2 = """
			public class A {
				public <T> void m(java.util.List<T> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<? extends T>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	// §4.5.1 rule 7: T <= ? extends T; for any fixed T, v2's List<? extends T> is wider than v1's List<T>
	// — all v1 call sites remain valid but it breaks implementers
	@Client("""
		new A().<Number>m(java.util.List.<Number>of());
		new A() {
			@Override public <T> void m(java.util.List<T> l) {}
		}.m(java.util.List.<Number>of());""")
	@Test
	void generic_method_wildcard_added() {
		var v1 = """
			public class A {
				public <T> void m(java.util.List<T> l) {}
			}""";
		var v2 = """
			public class A {
				public <T> void m(java.util.List<? extends T> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<T>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().<Number>m(java.util.List.<Number>of());")
	@Test
	void generic_method_wildcard_added_final() {
		var v1 = """
			public final class A {
				public <T> void m(java.util.List<T> l) {}
			}""";
		var v2 = """
			public final class A {
				public <T> void m(java.util.List<? extends T> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// Source-breaking: a subclass overriding m(List<Object>) gets a name clash with m(List<T>),
	// because List<Object> is not a subsignature of List<T> (erasure is raw List, not List<Object>).
	@Client("""
		new A().m(java.util.List.<Object>of());
		new A() {
			@Override public void m(java.util.List<Object> l) {}
		}.m(java.util.List.<Object>of());""")
	@Test
	void type_first_param_added_and_used_as_part_of_method_parameter_type() {
		var v1 = """
			public class A {
				public void m(java.util.List<Object> l) {}
			}""";
		var v2 = """
			public class A<T> {
				public void m(java.util.List<T> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<java.lang.Object>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<Object>of());")
	@Test
	void type_first_param_added_and_used_as_part_of_method_parameter_type_final() {
		var v1 = """
			public class A {
				public final void m(java.util.List<Object> l) {}
			}""";
		var v2 = """
			public class A<T> {
				public final void m(java.util.List<T> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// Source-breaking: a subclass overriding m(List<Object>) gets a name clash with m(List<T>),
	// because List<Object> is not a subsignature of List<T> (erasure is raw List, not List<Object>).
	@Client("""
		new A().m(java.util.List.<Object>of());
		new A() {
			@Override public void m(java.util.List<Object> l) {}
		}.m(java.util.List.<Object>of());""")
	@Test
	void method_first_param_added_and_used_as_part_of_parameter_type() {
		var v1 = """
			public class A {
				public void m(java.util.List<Object> l) {}
			}""";
		var v2 = """
			public class A {
				public <T> void m(java.util.List<T> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<java.lang.Object>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<Object>of());")
	@Test
	void method_first_param_added_and_used_as_part_of_parameter_type_final() {
		var v1 = """
			public class A {
				public final void m(java.util.List<Object> l) {}
			}""";
		var v2 = """
			public class A {
				public final <T> void m(java.util.List<T> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §4.8: a type that becomes generic is used raw by existing clients, who see the erasure of its members
	@Client("""
		new A().m(java.util.List.<String>of());
		new A() {
			@Override public void m(java.util.List l) {}
		};""")
	@Test
	void type_becomes_generic_raw_parameter_parameterized() {
		var v1 = """
			public class A {
				public void m(java.util.List l) {}
			}""";
		var v2 = """
			public class A<T> {
				public void m(java.util.List<T> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<String>of());")
	@Test
	void type_becomes_generic_raw_parameter_parameterized_final() {
		var v1 = """
			public class A {
				public final void m(java.util.List l) {}
			}""";
		var v2 = """
			public class A<T> {
				public final void m(java.util.List<T> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		new A(String.class);
		new A(Integer.class) {};""")
	@Test
	void type_becomes_generic_raw_constructor_parameter_parameterized() {
		var v1 = """
			public class A {
				public A(Class c) {}
			}""";
		var v2 = """
			public class A<T extends CharSequence> {
				public A(Class<T> c) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<String>of(), java.util.List.<Integer>of());")
	@Test
	void type_becomes_generic_raw_varargs_parameter_parameterized() {
		var v1 = """
			public class A {
				public void m(java.util.List... l) {}
			}""";
		var v2 = """
			public class A<T> {
				public void m(java.util.List<T>... l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §4.8: the type parameters of a generic method of a raw type are erased too
	@Client("""
		new A().m(java.util.List.<String>of());
		new A() {
			@Override public void m(java.util.List l) {}
		};""")
	@Test
	void type_becomes_generic_raw_parameter_parameterized_with_method_type_parameter() {
		var v1 = """
			public class A {
				public void m(java.util.List l) {}
			}""";
		var v2 = """
			public class A<T> {
				public <U extends T> void m(java.util.List<U> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §4.8: the erasure applies to the whole member type, not only to the new type parameters
	@Client("new A().m(java.util.List.<Integer>of());")
	@Test
	void type_becomes_generic_raw_parameter_unrelated_type_argument() {
		var v1 = """
			public class A {
				public void m(java.util.List l) {}
			}""";
		var v2 = """
			public class A<T> {
				public void m(java.util.List<String> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §4.8: the type of a static member of a raw type is not erased
	@Client("A.m(java.util.List.<Integer>of());")
	@Test
	void type_becomes_generic_raw_static_parameter_parameterized() {
		var v1 = """
			public class A {
				public static void m(java.util.List l) {}
			}""";
		var v2 = """
			public class A<T> {
				public static void m(java.util.List<String> l) {}
			}""";

		assertBC("A", "A.m(java.util.List)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	// A type that was already generic is not only used raw
	@Client("new A<String>().m(java.util.List.<Integer>of());")
	@Test
	void generic_type_raw_parameter_parameterized() {
		var v1 = """
			public class A<T> {
				public void m(java.util.List l) {}
			}""";
		var v2 = """
			public class A<T> {
				public void m(java.util.List<T> l) {}
			}""";

		assertBC("A", "A.m(java.util.List)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	// §4.8, §8.4.2: m(List<String>) is not a subsignature of the erased m(List), even if m did not change
	@Client("""
		new A() {
			@Override public void m(java.util.List<String> l) {}
		};""")
	@Test
	void type_becomes_generic_unchanged_parameterized_parameter() {
		var v1 = """
			public class A {
				public void m(java.util.List<String> l) {}
			}""";
		var v2 = """
			public class A<T> {
				public void m(java.util.List<String> l) {}
			}""";

		assertBC("A", "A.m(java.util.List<java.lang.String>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(java.util.List.<String>of());")
	@Test
	void type_becomes_generic_parameterized_parameter_final() {
		var v1 = """
			public class A {
				public final void m(java.util.List<String> l) {}
				public A(java.util.List<String> l) {}
			}""";
		var v2 = """
			public class A<T> {
				public final void m(java.util.List<T> l) {}
				public A(java.util.List<T> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §8.4.2: m(List, List<String>) is not a subsignature of the erased m(List, List)
	@Client("""
		new A() {
			@Override public void m(java.util.List l1, java.util.List<String> l2) {}
		};""")
	@Test
	void type_becomes_generic_raw_parameter_parameterized_next_to_parameterized_parameter() {
		var v1 = """
			public class A {
				public void m(java.util.List l1, java.util.List<String> l2) {}
			}""";
		var v2 = """
			public class A<T> {
				public void m(java.util.List<T> l1, java.util.List<String> l2) {}
			}""";

		assertBC("A", "A.m(java.util.List,java.util.List<java.lang.String>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new B().m("");
		new B() { @Override public void m(Object o) {} };""")
	@Test
	void supertype_generified() {
		var v1 = """
			public class A { public void m(Object o) {} }
			public class B extends A {}""";
		var v2 = """
			public class A<T> { public void m(T t) {} }
			public class B extends A<String> {}""";

		// The descriptor remains A.m(Object): not removed
		assertBC("B", "A.m(java.lang.Object)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("new B().m(\"\");")
	@Test
	void supertype_type_argument_changed() {
		var v1 = """
			public class A<T> { public void m(T t) {} }
			public class B extends A<String> {}""";
		var v2 = """
			public class A<T> { public void m(T t) {} }
			public class B extends A<Integer> {}""";

		assertBC("B", "A.m(java.lang.String)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("new B().m(new Object());")
	@Test
	void supertype_generified_next_to_an_overload() {
		var v1 = """
			public class A { public void m(Object o) {} public void m(String s) {} }
			public class B extends A {}""";
		var v2 = """
			public class A<T> { public void m(T t) {} public void m(String s) {} }
			public class B extends A<Integer> {}""";

		assertBC("B", "A.m(java.lang.Object)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1, buildDiff(v1, v2));
	}

	// Arrays as substituted type arguments: assertj's S isSameAs(A) inherited as isSameAs(boolean[])
	@Client("new B() { @Override public B m(boolean[] a) { return this; } };")
	@Test
	void inherited_array_type_argument_becomes_object() {
		var v1 = """
			abstract class A<S extends A<S, T>, T> { public S m(T t) { return null; } }
			public class B extends A<B, boolean[]> {}""";
		var v2 = """
			abstract class A<S extends A<S, T>, T> { public S m(Object o) { return null; } }
			public class B extends A<B, boolean[]> {}""";

		assertBC("B", "A.m(boolean[])", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1,
			buildDiff(v1, v2));
	}

	// Final: callers only; Object accepts every boolean[]
	@Client("new B().m(new boolean[0]);")
	@Test
	void inherited_array_type_argument_becomes_object_final() {
		var v1 = """
			abstract class A<S extends A<S, T>, T> { public final S m(T t) { return null; } }
			public class B extends A<B, boolean[]> {}""";
		var v2 = """
			abstract class A<S extends A<S, T>, T> { public final S m(Object o) { return null; } }
			public class B extends A<B, boolean[]> {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// assertj's final S isIn(Object...) becoming isIn(A...)
	@Client("new B().m(\"\");")
	@Test
	void final_object_varargs_become_type_argument() {
		var v1 = """
			abstract class A<S extends A<S, T>, T> { public final S m(Object... o) { return null; } }
			public class B extends A<B, Integer> {}""";
		var v2 = """
			abstract class A<S extends A<S, T>, T> { public final S m(T... t) { return null; } }
			public class B extends A<B, Integer> {}""";

		assertBC("B", "A.m(java.lang.Object[])", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1,
			buildDiff(v1, v2));
	}

	@Client("new A().m(new java.util.List[0]);")
	@Test
	void raw_array_component_becomes_unbounded_wildcard_final() {
		var v1 = """
			public class A { public final void m(java.util.List[] l) {} }""";
		var v2 = """
			public class A { public final void m(java.util.List<?>[] l) {} }""";

		assertNoBC(buildDiff(v1, v2));
	}

	// Neither type has type arguments: not a raw type becoming parameterized
	@Client("new B().m(1);")
	@Test
	void final_object_becomes_type_argument() {
		var v1 = """
			abstract class A<S extends A<S, T>, T> { public final S m(Object o) { return null; } }
			public class B extends A<B, String> {}""";
		var v2 = """
			abstract class A<S extends A<S, T>, T> { public final S m(T t) { return null; } }
			public class B extends A<B, String> {}""";

		assertBC("B", "A.m(java.lang.Object)", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1,
			buildDiff(v1, v2));
	}

	// joda-time: a final class implementing raw Comparable becomes Comparable<P>
	@Client("new P().compareTo(\"\");")
	@Test
	void raw_comparable_generified_in_final_class() {
		var v1 = """
			public final class P implements Comparable { public int compareTo(Object o) { return 0; } }""";
		var v2 = """
			public final class P implements Comparable<P> { public int compareTo(P o) { return 0; } }""";

		assertBC("P", "P.compareTo(java.lang.Object)", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1,
			buildDiff(v1, v2));
	}

	// Same parameter, only its declaration changes: an array becoming variadic
	@Client("new A().m(new String[] {\"a\"});")
	@Test
	void array_becomes_varargs() {
		var v1 = """
			public class A { public void m(String[] a) {} }""";
		var v2 = """
			public class A { public void m(String... a) {} }""";

		assertNoBC(buildDiff(v1, v2));
	}

	// The erasure of <T> T[] toArray(T[]) is the old signature: callers and overriders still compile
	@Client("""
		Object[] r = new A().toArray(new String[0]);
		new A() { @Override public Object[] toArray(Object[] a) { return a; } };""")
	@Test
	void array_parameter_becomes_generic_method_array() {
		var v1 = """
			public class A { public Object[] toArray(Object[] a) { return a; } }""";
		var v2 = """
			public class A { public <T> T[] toArray(T[] a) { return a; } }""";

		assertNoBC(buildDiff(v1, v2));
	}

	// assertj's ObjectArrayAssert<T> extends AbstractAssert<..., T[]>: isSameAs(T[]) becoming isSameAs(Object)
	@Client("new B<String>() { @Override public B<String> m(String[] a) { return this; } };")
	@Test
	void inherited_array_of_type_variable_becomes_object() {
		var v1 = """
			abstract class A<S extends A<S, U>, U> { public S m(U u) { return null; } }
			public class B<T> extends A<B<T>, T[]> {}""";
		var v2 = """
			abstract class A<S extends A<S, U>, U> { public S m(Object o) { return null; } }
			public class B<T> extends A<B<T>, T[]> {}""";

		assertBC("B", "A.m(T[])", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1, buildDiff(v1, v2));
	}

	// spring's ScriptFactory.getScriptedObject(ScriptSource, Class[]) becoming (ScriptSource, Class<?>...): Class[] is
	// the erasure of Class<?>[], so implementations keeping Class[] still override it
	@Client("new F() { @Override public Object get(Class[] c) { return null; } }.get(new Class[0]);")
	@Test
	void raw_array_becomes_unbounded_wildcard_varargs_overridable() {
		var v1 = """
			public abstract class F { public abstract Object get(Class[] c); }""";
		var v2 = """
			public abstract class F { public abstract Object get(Class<?>... c); }""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §15.27.3: the parameters of a lambda get the parameter types of the interface method it implements
	@Client("F f = c -> { Class<String> s = c[0]; return s; };")
	@Test
	void raw_array_becomes_unbounded_wildcard_varargs_interface() {
		var v1 = """
			public interface F { Object get(Class[] c); }""";
		var v2 = """
			public interface F { Object get(Class<?>... c); }""";

		assertBC("F", "F.get(java.lang.Class[])", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1,
			buildDiff(v1, v2));
	}

	@Client("I i = l -> l.add(1);")
	@Test
	void raw_to_unbounded_wildcard_interface() {
		var v1 = """
			public interface I { void m(java.util.List l); }""";
		var v2 = """
			public interface I { void m(java.util.List<?> l); }""";

		assertBC("I", "I.m(java.util.List)", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1,
			buildDiff(v1, v2));
	}

	// A lambda can implement m through a subinterface that implements n
	@Client("""
		interface J extends I { default void n() {} }
		J j = l -> l.add(1);""")
	@Test
	void raw_to_unbounded_wildcard_interface_with_several_abstract_methods() {
		var v1 = """
			public interface I { void m(java.util.List l); void n(); }""";
		var v2 = """
			public interface I { void m(java.util.List<?> l); void n(); }""";

		assertBC("I", "I.m(java.util.List)", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1,
			buildDiff(v1, v2));
	}

	@Client("new I() { @Override public void m(java.util.List l) { l.add(1); } }.m(java.util.List.<String>of());")
	@Test
	void raw_to_unbounded_wildcard_default_method() {
		var v1 = """
			public interface I { default void m(java.util.List l) {} }""";
		var v2 = """
			public interface I { default void m(java.util.List<?> l) {} }""";

		assertNoBC(buildDiff(v1, v2));
	}

	// §8.4.2: m(T) is neither m(Object) nor its erasure, so overriders in generic subclasses get a name clash
	@Client("class C<T> extends A<T> { @Override public void m(T t) {} }")
	@Test
	void type_variable_becomes_object() {
		var v1 = """
			public class A<T> { public void m(T t) {} }""";
		var v2 = """
			public class A<T> { public void m(Object o) {} }""";

		assertBC("A", "A.m(T)", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("new A<String>().m(\"\");")
	@Test
	void type_variable_becomes_object_final() {
		var v1 = """
			public class A<T> { public final void m(T t) {} }""";
		var v2 = """
			public class A<T> { public final void m(Object o) {} }""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A<String>().m(new Object());")
	@Test
	void object_becomes_type_variable_final() {
		var v1 = """
			public class A<T> { public final void m(Object o) {} }""";
		var v2 = """
			public class A<T> { public final void m(T t) {} }""";

		assertBC("A", "A.m(java.lang.Object)", BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1,
			buildDiff(v1, v2));
	}

	// §8.4.2: m(List<String>) is neither m(List<T>) nor its erasure
	@Client("new A() { @Override public void m(java.util.List<String> l) {} };")
	@Test
	void parameterized_becomes_method_type_parameter() {
		var v1 = """
			public class A { public void m(java.util.List<String> l) {} }""";
		var v2 = """
			public class A { public <T> void m(java.util.List<T> l) {} }""";

		assertBC("A", "A.m(java.util.List<java.lang.String>)",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1, buildDiff(v1, v2));
	}

	// §4.8, §8.4.2: m(List<String>[]) is not a subsignature of the erased m(List[]), even if m did not change
	@Client("new A() { @Override public void m(java.util.List<String>[] l) {} };")
	@Test
	void type_becomes_generic_unchanged_parameterized_array_parameter() {
		var v1 = """
			public class A { public void m(java.util.List<String>[] l) {} }""";
		var v2 = """
			public class A<T> { public void m(java.util.List<String>[] l) {} }""";

		assertBC("A", "A.m(java.util.List[])",
			BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 1, buildDiff(v1, v2));
	}
}
