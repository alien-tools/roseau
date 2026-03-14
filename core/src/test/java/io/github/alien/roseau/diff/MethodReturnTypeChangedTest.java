package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodReturnTypeChangedTest {
	@Client("new A().m();")
	@Test
	void void_to_non_void_binary_and_source() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public int m() { return 0; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("int i = new A().m();")
	@Test
	void non_void_to_void_binary_and_source() {
		var v1 = """
			public class A {
				public int m() { return 0; }
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("int i = new A().m();")
	@Test
	void boxing_non_final_binary_and_source() {
		var v1 = """
			public class A {
				public int m() { return 0; }
			}""";
		var v2 = """
			public class A {
				public Integer m() { return 0; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("int i = new A().m();")
	@Test
	void boxing_final_binary_only() {
		var v1 = """
			public class A {
				public final int m() { return 0; }
			}""";
		var v2 = """
			public class A {
				public final Integer m() { return 0; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2));
	}

	@Client("Integer i = new A().m();")
	@Test
	void unboxing_non_final_binary_and_source() {
		var v1 = """
			public class A {
				public Integer m() { return 0; }
			}""";
		var v2 = """
			public class A {
				public int m() { return 0; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("int i = new A().m();")
	@Test
	void primitive_widening_binary_and_source() {
		var v1 = """
			public class A {
				public int m() { return 0; }
			}""";
		var v2 = """
			public class A {
				public long m() { return 0L; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("long l = new A().m();")
	@Test
	void primitive_narrowing_binary_and_source() {
		var v1 = """
			public class A {
				public long m() { return 0L; }
			}""";
		var v2 = """
			public class A {
				public int m() { return 0; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("Double d = new A().m();")
	@Test
	void primitive_narrowing_boxing_context_binary_and_source() {
		var v1 = """
			public class A {
				public final double m() { return 0D; }
			}""";
		var v2 = """
			public class A {
				public final int m() { return 0; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("java.io.InputStream is = new A().m();")
	@Test
	void reference_subtype_non_final_binary_and_source() {
		var v1 = """
			public class A {
				public java.io.InputStream m() { return null; }
			}""";
		var v2 = """
			public class A {
				public java.io.FileInputStream m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("java.io.InputStream is = new A().m();")
	@Test
	void reference_subtype_final_binary_only() {
		var v1 = """
			public class A {
				public final java.io.InputStream m() { return null; }
			}""";
		var v2 = """
			public class A {
				public final java.io.FileInputStream m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2));
	}

	@Client("java.io.FileInputStream fis = new A().m();")
	@Test
	void reference_supertype_non_final_binary_and_source() {
		var v1 = """
			public class A {
				public java.io.FileInputStream m() { return null; }
			}""";
		var v2 = """
			public class A {
				public java.io.InputStream m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("Object o = new A().m();")
	@Test
	void method_first_param_added_and_used_as_direct_return_no_break() {
		var v1 = """
			public class A {
				public Object m() { return null; }
			}""";
		var v2 = """
			public class A {
				public <T> T m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("java.util.List<Object> l = new A().m();")
	@Test
	void method_first_param_added_and_used_as_nested_return_no_break() {
		var v1 = """
			public class A {
				public java.util.List<Object> m() { return null; }
			}""";
		var v2 = """
			public class A {
				public <T> java.util.List<T> m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("Number n = new A().m();")
	@Test
	void method_bounded_param_added_and_used_as_direct_return_no_break() {
		var v1 = """
			public class A {
				public Number m() { return null; }
			}""";
		var v2 = """
			public class A {
				public <T extends Number> T m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("String s = new A().<String>m();")
	@Test
	void method_formal_type_parameter_renamed_direct_return_no_break() {
		var v1 = """
			public class A {
				public <T> T m() { return null; }
			}""";
		var v2 = """
			public class A {
				public <U> U m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("java.util.List<String> l = new A().<String>m();")
	@Test
	void method_formal_type_parameter_renamed_nested_return_no_break() {
		var v1 = """
			public class A {
				public <T> java.util.List<T> m() { return null; }
			}""";
		var v2 = """
			public class A {
				public <U> java.util.List<U> m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("Integer i = new A<Integer, String>().m();")
	@Test
	void unrelated_type_parameters_source_only() {
		var v1 = """
			public class A<T, U> {
				public T m() { return null; }
			}""";
		var v2 = """
			public class A<T, U> {
				public U m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("String s = new A<String>().m();")
	@Test
	void type_formal_type_parameter_renamed_direct_return_no_break() {
		var v1 = """
			public class A<T> {
				public T m() { return null; }
			}""";
		var v2 = """
			public class A<U> {
				public U m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("Object o = new A().m();")
	@Test
	void type_first_param_added_and_used_as_direct_return_no_break() {
		var v1 = """
			public class A {
				public Object m() { return null; }
			}""";
		var v2 = """
			public class A<T> {
				public T m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("java.util.List<String> l = new A<String>().m();")
	@Test
	void type_formal_type_parameter_renamed_nested_return_no_break() {
		var v1 = """
			public class A<T> {
				public java.util.List<T> m() { return null; }
			}""";
		var v2 = """
			public class A<U> {
				public java.util.List<U> m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("java.util.List<Object> l = new A().m();")
	@Test
	void type_first_param_added_and_used_as_nested_return_no_break() {
		var v1 = """
			public class A {
				public java.util.List<Object> m() { return null; }
			}""";
		var v2 = """
			public class A<T> {
				public java.util.List<T> m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("Number n = new A().m();")
	@Test
	void type_bounded_param_added_and_used_as_direct_return_no_break() {
		var v1 = """
			public class A {
				public Number m() { return null; }
			}""";
		var v2 = """
			public class A<T extends Number> {
				public T m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A<CharSequence, String>().m();")
	@Test
	void subtype_type_parameter_non_final_source_only() {
		var v1 = """
			public class A<T, U extends T> {
				public T m() { return null; }
			}""";
		var v2 = """
			public class A<T, U extends T> {
				public U m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("CharSequence cs = new A<CharSequence, String>().m();")
	@Test
	void subtype_type_parameter_final_no_break() {
		var v1 = """
			public class A<T, U extends T> {
				public final T m() { return null; }
			}""";
		var v2 = """
			public class A<T, U extends T> {
				public final U m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("String s = new A<CharSequence, String>().m();")
	@Test
	void supertype_type_parameter_source_only() {
		var v1 = """
			public class A<T, U extends T> {
				public U m() { return null; }
			}""";
		var v2 = """
			public class A<T, U extends T> {
				public T m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("java.util.List<Integer> l = new A().m();")
	@Test
	void invariant_generic_argument_change_source_only() {
		var v1 = """
			public class A {
				public java.util.List<Integer> m() { return null; }
			}""";
		var v2 = """
			public class A {
				public java.util.List<String> m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("java.util.List<String> l = new A().m();")
	@Test
	void generic_erasure_change_final_binary_only() {
		var v1 = """
			public class A {
				public final java.util.List<String> m() { return null; }
			}""";
		var v2 = """
			public class A {
				public final java.util.ArrayList<String> m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2));
	}

	@Client("java.util.List<String> l = new A().m();")
	@Test
	void raw_subtype_to_parameterized_supertype_final_binary_only() {
		var v1 = """
			public class A {
				public final java.util.List<String> m() { return null; }
			}""";
		var v2 = """
			public class A {
				public final java.util.ArrayList m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2));
	}

	@Client("int[] a = new A().m();")
	@Test
	void array_incompatible_binary_and_source() {
		var v1 = """
			public class A {
				public int[] m() { return new int[] { 0 }; }
			}""";
		var v2 = """
			public class A {
				public String[] m() { return new String[] { null }; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("java.io.InputStream[] a = new A().m();")
	@Test
	void array_subtype_non_final_binary_and_source() {
		var v1 = """
			public class A {
				public java.io.InputStream[] m() { return new java.io.InputStream[] { null }; }
			}""";
		var v2 = """
			public class A {
				public java.io.FileInputStream[] m() { return new java.io.FileInputStream[] { null }; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("java.io.FileInputStream[] a = new A().m();")
	@Test
	void array_supertype_non_final_binary_and_source() {
		var v1 = """
			public class A {
				public java.io.FileInputStream[] m() { return new java.io.FileInputStream[] { null }; }
			}""";
		var v2 = """
			public class A {
				public java.io.InputStream[] m() { return new java.io.InputStream[] { null }; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("@A(0) class X {}")
	@Test
	void annotation_interface_method_binary_and_source() {
		var v1 = """
			public @interface A {
				int value();
			}""";
		var v2 = """
			public @interface A {
				String value();
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.value()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.value()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("int i = new B().m();")
	@Test
	void changed_in_super_type_reports_on_subtype_too() {
		var v1 = """
			public class A {
				public int m() { return 0; }
			}
			public class B extends A {}""";
		var v2 = """
			public class A {
				public String m() { return null; }
			}
			public class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2),
			bc("B", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("B", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("B.m();")
	@Test
	void inherited_static_method_changed_binary_and_source() {
		var v1 = """
			class A {
				public static int m() { return 0; }
			}
			public class B extends A {}""";
		var v2 = """
			class A {
				public static String m() { return null; }
			}
			public class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("B", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("B", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Test
	void to_unknown_binary_and_source() {
		var v1 = """
			public class A {
				public A m() { return null; }
			}""";
		var v2 = """
			public class A {
				public Unknown m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Test
	void from_unknown_binary_and_source() {
		var v1 = """
			public class A {
				public Unknown m() { return null; }
			}""";
		var v2 = """
			public class A {
				public A m() { return null; }
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("java.util.Map<String, java.util.List<String>> m = new A<String>().m();")
	@Test
	void return_type_deeply_nested_type_param_renamed_no_break() {
		var v1 = """
			public class A<T> {
				public java.util.Map<T, java.util.List<T>> m() { return null; }
			}""";
		var v2 = """
			public class A<U> {
				public java.util.Map<U, java.util.List<U>> m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("java.util.Map<String, Integer> m = new A<String>().<Integer>m();")
	@Test
	void return_type_both_type_and_exec_params_renamed_no_break() {
		var v1 = """
			public class A<T> {
				public <U> java.util.Map<T, U> m() { return null; }
			}""";
		var v2 = """
			public class A<V> {
				public <W> java.util.Map<V, W> m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
