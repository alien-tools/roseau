package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class FormalTypeParameterChangedTest {
	@Client("A<String> a = new A<>();")
	@Test
	void param_renamed() {
		var v1 = "public class A<T> {}";
		var v2 = "public class A<U> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String, Integer> a = new A<>();")
	@Test
	void param_swapped() {
		var v1 = "public class A<T, U> {}";
		var v2 = "public class A<U, T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().<String>m(\"\");")
	@Test
	void method_param_renamed_used_as_parameter_type() {
		var v1 = """
			public class A {
				public <T> void m(T t) {}
			}""";
		var v2 = """
			public class A {
				public <U> void m(U u) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A<String>().m(\"\");")
	@Test
	void type_param_renamed_used_as_method_parameter_type() {
		var v1 = """
			public class A<T> {
				public void m(T t) {}
			}""";
		var v2 = """
			public class A<U> {
				public void m(U u) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().<String>m(java.util.List.of(\"\"));")
	@Test
	void method_param_renamed_used_as_part_of_parameter_type() {
		var v1 = """
			public class A {
				public <T> void m(java.util.List<T> t) {}
			}""";
		var v2 = """
			public class A {
				public <U> void m(java.util.List<U> u) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A<String>().m(java.util.List.of(\"\"));")
	@Test
	void type_param_renamed_used_as_part_of_method_parameter_type() {
		var v1 = """
			public class A<T> {
				public void m(java.util.List<T> t) {}
			}""";
		var v2 = """
			public class A<U> {
				public void m(java.util.List<U> u) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String, CharSequence> a = new A<>();")
	@Test
	void bounded_param_swapped() {
		var v1 = "public class A<T extends String, U extends CharSequence> {}";
		var v2 = "public class A<U extends CharSequence, T extends String> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<Object> a = new A<>();")
	@Test
	void bound_added() {
		var v1 = "public class A<T> {}";
		var v2 = "public class A<T extends String> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<Object> a = new A<>();")
	@Test
	void bound_object_added() {
		var v1 = "public class A<T> {}";
		var v2 = "public class A<T extends Object> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String> a = new A<>();")
	@Test
	void bound_removed() {
		var v1 = "public class A<T extends String> {}";
		var v2 = "public class A<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		abstract class X implements CharSequence, Runnable {}
		A<X> a = new A<>();
		""")
	@Test
	void second_bound_removed() {
		var v1 = "public class A<T extends CharSequence & Runnable> {}";
		var v2 = "public class A<T extends CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String, CharSequence> a = new A<>();")
	@Test
	void bound_param_removed() {
		var v1 = "public class A<T extends U, U> {}";
		var v2 = "public class A<T, U> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<Integer, Number> a = new A<>();")
	@Test
	void bound_param_added() {
		var v1 = "public class A<T, U> {}";
		var v2 = "public class A<T, U extends T> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<Number, Number> a = new A<>();")
	@Test
	void bound_param_replaced_with_equivalent_resolved_bound() {
		var v1 = "public class A<T extends Number, U extends T> {}";
		var v2 = "public class A<T extends Number, U extends Number> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String> a = new A<>();")
	@Test
	void bound_modified_compatible() {
		var v1 = "public class A<T extends String> {}";
		var v2 = "public class A<T extends CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<StringBuilder> a = new A<>();")
	@Test
	void bound_modified_incompatible() {
		var v1 = "public class A<T extends CharSequence> {}";
		var v2 = "public class A<T extends String> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<String, StringBuilder, String> a = new A<>();")
	@Test
	void bound_modified_incompatible_param_1() {
		// Still breaking if client chooses a U that is not a subtype of T
		var v1 = "public class A<T extends String, U extends CharSequence, V extends T> {}";
		var v2 = "public class A<T extends String, U extends CharSequence, V extends U> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<String, CharSequence, CharSequence> a = new A<>();")
	@Test
	void bound_modified_incompatible_param_2() {
		var v1 = "public class A<T extends String, U extends CharSequence, V extends U> {}";
		var v2 = "public class A<T extends String, U extends CharSequence, V extends T> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<String> a = new A<>();")
	@Test
	void second_bound_added_compatible() {
		var v1 = "public class A<T extends String> {}";
		var v2 = "public class A<T extends String & CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String> a = new A<>();")
	@Test
	void second_bound_added_incompatible() {
		var v1 = "public class A<T extends String> {}";
		var v2 = "public class A<T extends String & Runnable> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_changed_to_compatible_generic() {
		var v1 = "public class A<T extends java.util.List<? extends String>> {}";
		var v2 = "public class A<T extends java.util.List<? extends CharSequence>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<CharSequence>> a = new A<>();")
	@Test
	void bound_changed_to_incompatible_generic() {
		var v1 = "public class A<T extends java.util.List<? extends CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<? extends String>> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.ArrayList<String>> a = new A<>();")
	@Test
	void bound_changed_to_generic_supertype() {
		var v1 = "public class A<T extends java.util.ArrayList<? extends String>> {}";
		var v2 = "public class A<T extends java.util.List<? extends CharSequence>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_changed_to_generic_subtype() {
		var v1 = "public class A<T extends java.util.List<? extends String>> {}";
		var v2 = "public class A<T extends java.util.ArrayList<? extends CharSequence>> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<CharSequence>> a = new A<>();")
	@Test
	void bound_changed_to_compatible_generic_super() {
		var v1 = "public class A<T extends java.util.List<? super CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<? super String>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_changed_to_incompatible_generic_super() {
		var v1 = "public class A<T extends java.util.List<? super String>> {}";
		var v2 = "public class A<T extends java.util.List<? super CharSequence>> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.ArrayList<CharSequence>> a = new A<>();")
	@Test
	void bound_changed_to_incompatible_type_super() {
		var v1 = "public class A<T extends java.util.ArrayList<? super CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<? super String>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<CharSequence>> a = new A<>();")
	@Test
	void bound_changed_to_incompatible_subtype_super() {
		var v1 = "public class A<T extends java.util.List<? super CharSequence>> {}";
		var v2 = "public class A<T extends java.util.ArrayList<? super String>> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_changed_to_generic_wildcard_extends() {
		var v1 = "public class A<T extends java.util.List<? extends CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<?>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<Object>> a = new A<>();")
	@Test
	void bound_changed_from_generic_wildcard_extends() {
		var v1 = "public class A<T extends java.util.List<?>> {}";
		var v2 = "public class A<T extends java.util.List<? extends CharSequence>> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<Object>> a = new A<>();")
	@Test
	void bound_changed_to_generic_wildcard_super() {
		var v1 = "public class A<T extends java.util.List<? super CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<?>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_changed_from_generic_wildcard_super() {
		var v1 = "public class A<T extends java.util.List<?>> {}";
		var v2 = "public class A<T extends java.util.List<? super CharSequence>> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_generic_wildcard_to_type() {
		var v1 = "public class A<T extends java.util.List<? extends CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<CharSequence>> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_type_to_compatible_wildcard() {
		var v1 = "public class A<T extends java.util.List<String>> {}";
		var v2 = "public class A<T extends java.util.List<? extends String>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<CharSequence>> a = new A<>();")
	@Test
	void bound_type_to_incompatible_wildcard() {
		var v1 = "public class A<T extends java.util.List<CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<? extends String>> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<CharSequence>, String> a = new A();")
	@Test
	void unchanged_type_params_bounds() {
		var v1 = "public class A<T extends java.util.List<? super U>, U> {}";
		var v2 = "public class A<T extends java.util.List<? super U>, U> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String> a = new A<>();")
	@Test
	void self_referencing_bound_renamed() {
		var v1 = "public class A<T extends Comparable<T>> {}";
		var v2 = "public class A<U extends Comparable<U>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<Number> a = new A<>();")
	@Test
	void self_referencing_bound_tightened() {
		var v1 = "public class A<T extends Number> {}";
		var v2 = "public class A<T extends Integer> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("new A().<String>m();")
	@Test
	void method_self_referencing_bound_renamed() {
		var v1 = """
			public class A {
				public <T extends Comparable<T>> void m() {}
			}""";
		var v2 = """
			public class A {
				public <U extends Comparable<U>> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().<String>m();")
	@Test
	void final_method_self_referencing_bound_renamed() {
		var v1 = """
			public class A {
				public final <T extends Comparable<T>> void m() {}
			}""";
		var v2 = """
			public class A {
				public final <U extends Comparable<U>> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().<Number>m();")
	@Test
	void method_self_referencing_bound_tightened() {
		var v1 = """
			public class A {
				public <T extends Number> void m() {}
			}""";
		var v2 = """
			public class A {
				public <T extends Integer> void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("A<String, String> a = new A<>();")
	@Test
	void cross_referencing_type_params_renamed() {
		var v1 = "public class A<T, U extends Comparable<T>> {}";
		var v2 = "public class A<X, Y extends Comparable<X>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<Integer> a = new A<>();")
	@Test
	void multi_bound_intersection_renamed() {
		var v1 = "public class A<T extends Number & Comparable<T>> {}";
		var v2 = "public class A<U extends Number & Comparable<U>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A<String>().<String>m();")
	@Test
	void simultaneous_type_and_exec_rename() {
		var v1 = """
			public class A<T> {
				public <U extends T> void m() {}
			}""";
		var v2 = """
			public class A<V> {
				public <W extends V> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A<String>().<String>m();")
	@Test
	void simultaneous_type_and_exec_rename_final() {
		var v1 = """
			public class A<T> {
				public final <U extends T> void m() {}
			}""";
		var v2 = """
			public class A<V> {
				public final <W extends V> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().<Integer>m();")
	@Test
	void multi_bound_intersection_renamed_method() {
		var v1 = """
			public class A {
				public <T extends Number & Comparable<T>> void m() {}
			}""";
		var v2 = """
			public class A {
				public <U extends Number & Comparable<U>> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().<Integer>m();")
	@Test
	void multi_bound_intersection_renamed_final_method() {
		var v1 = """
			public class A {
				public final <T extends Number & Comparable<T>> void m() {}
			}""";
		var v2 = """
			public class A {
				public final <U extends Number & Comparable<U>> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A<String>().m(java.util.List.of(\"\"));")
	@Test
	void param_generics_wildcard_bounds_type_param_renamed() {
		var v1 = """
			public class A<T> {
				public void m(java.util.List<? extends T> l) {}
			}""";
		var v2 = """
			public class A<U> {
				public void m(java.util.List<? extends U> l) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
