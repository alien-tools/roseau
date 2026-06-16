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
}
