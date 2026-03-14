package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class FieldTypeChangedTest {
	private static void assertBinaryOnly(String type, String field, int line, String v1, String v2) {
		assertBCs(buildDiff(v1, v2), bc(type, field, BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED, line));
	}

	private static void assertSourceOnly(String type, String field, int line, String v1, String v2) {
		assertBCs(buildDiff(v1, v2), bc(type, field, BreakingChangeKind.FIELD_TYPE_CHANGED_INCOMPATIBLE, line));
	}

	private static void assertBinaryAndSource(String type, String field, int line, String v1, String v2) {
		assertBCs(buildDiff(v1, v2),
			bc(type, field, BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED, line),
			bc(type, field, BreakingChangeKind.FIELD_TYPE_CHANGED_INCOMPATIBLE, line));
	}

	@Client("int i = new A().f;")
	@Test
	void boxing_binary_only() {
		assertBinaryOnly("A", "A.f", 2,
			"""
				public class A {
					public int f;
				}""",
			"""
				public class A {
					public Integer f;
				}"""
		);
	}

	@Client("Integer i = new A().f;")
	@Test
	void unboxing_binary_only() {
		assertBinaryOnly("A", "A.f", 2,
			"""
				public class A {
					public Integer f;
				}""",
			"""
				public class A {
					public int f;
				}"""
		);
	}

	@Client("int i = new A().f;")
	@Test
	void primitive_widening_binary_and_source() {
		assertBinaryAndSource("A", "A.f", 2,
			"""
				public class A {
					public int f;
				}""",
			"""
				public class A {
					public long f;
				}"""
		);
	}

	@Client("long l = new A().f;")
	@Test
	void primitive_narrowing_binary_and_source() {
		assertBinaryAndSource("A", "A.f", 2,
			"""
				public class A {
					public long f;
				}""",
			"""
				public class A {
					public int f;
				}"""
		);
	}

	@Client("java.io.InputStream is = new A().f;")
	@Test
	void reference_subtype_non_final_binary_and_source() {
		assertBinaryAndSource("A", "A.f", 2,
			"""
				public class A {
					public java.io.InputStream f;
				}""",
			"""
				public class A {
					public java.io.FileInputStream f;
				}"""
		);
	}

	@Client("java.io.InputStream is = new A().f;")
	@Test
	void reference_subtype_final_binary_only() {
		assertBinaryOnly("A", "A.f", 2,
			"""
				public class A {
					public final java.io.InputStream f = null;
				}""",
			"""
				public class A {
					public final java.io.FileInputStream f = null;
				}"""
		);
	}

	@Client("java.io.FileInputStream fis = new A().f;")
	@Test
	void reference_supertype_non_final_binary_and_source() {
		assertBinaryAndSource("A", "A.f", 2,
			"""
				public class A {
					public java.io.FileInputStream f;
				}""",
			"""
				public class A {
					public java.io.InputStream f;
				}"""
		);
	}

	@Client("Object f = new A().f;")
	@Test
	void type_first_param_added_and_used_as_direct_field_type_no_break() {
		var v1 = """
			public class A {
				public Object f;
			}""";
		var v2 = """
			public class A<T> {
				public T f;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("java.util.List<Object> f = new A().f;")
	@Test
	void type_first_param_added_and_used_as_nested_field_type_no_break() {
		var v1 = """
			public class A {
				public java.util.List<Object> f;
			}""";
		var v2 = """
			public class A<T> {
				public java.util.List<T> f;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("Number f = new A().f;")
	@Test
	void type_bounded_param_added_and_used_as_direct_field_type_no_break() {
		var v1 = """
			public class A {
				public Number f;
			}""";
		var v2 = """
			public class A<T extends Number> {
				public T f;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("String f = new A<String>().f;")
	@Test
	void type_formal_type_parameter_renamed_direct_field_type_no_break() {
		var v1 = """
			public class A<T> {
				public T f;
			}""";
		var v2 = """
			public class A<U> {
				public U f;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("java.util.List<String> f = new A<String>().f;")
	@Test
	void type_formal_type_parameter_renamed_nested_field_type_no_break() {
		var v1 = """
			public class A<T> {
				public java.util.List<T> f;
			}""";
		var v2 = """
			public class A<U> {
				public java.util.List<U> f;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("java.io.InputStream[] a = new A().f;")
	@Test
	void array_subtype_non_final_binary_and_source() {
		assertBinaryAndSource("A", "A.f", 2,
			"""
				public class A {
					public java.io.InputStream[] f;
				}""",
			"""
				public class A {
					public java.io.FileInputStream[] f;
				}"""
		);
	}

	@Client("java.io.InputStream[] a = new A().f;")
	@Test
	void array_subtype_final_binary_only() {
		assertBinaryOnly("A", "A.f", 2,
			"""
				public class A {
					public final java.io.InputStream[] f = null;
				}""",
			"""
				public class A {
					public final java.io.FileInputStream[] f = null;
				}"""
		);
	}

	@Client("int[] a = new A().f;")
	@Test
	void array_primitive_component_change_binary_and_source() {
		assertBinaryAndSource("A", "A.f", 2,
			"""
				public class A {
					public int[] f;
				}""",
			"""
				public class A {
					public long[] f;
				}"""
		);
	}

	@Client("Object o = new A().f;")
	@Test
	void array_to_object_final_binary_only() {
		assertBinaryOnly("A", "A.f", 2,
			"""
				public class A {
					public final Object f = null;
				}""",
			"""
				public class A {
					public final String[] f = null;
				}"""
		);
	}

	@Client("java.util.List<String> l = new A().f;")
	@Test
	void generic_invariant_argument_change_source_only() {
		assertSourceOnly("A", "A.f", 2,
			"""
				public class A {
					public java.util.List<String> f;
				}""",
			"""
				public class A {
					public java.util.List<Integer> f;
				}"""
		);
	}

	@Client("new A().f = java.util.List.of(1D);")
	@Test
	void generic_wildcard_non_final_source_only() {
		assertSourceOnly("A", "A.f", 2,
			"""
				public class A {
					public java.util.List<? extends Number> f;
				}""",
			"""
				public class A {
					public java.util.List<Integer> f;
				}"""
		);
	}

	@Client("java.util.List<? extends Number> l = new A().f;")
	@Test
	void generic_wildcard_final_no_break() {
		var v1 = """
			public class A {
				public final java.util.List<? extends Number> f = null;
			}""";
		var v2 = """
			public class A {
				public final java.util.List<Integer> f = null;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().f = java.util.List.of(\"\");")
	@Test
	void generic_erasure_change_non_final_binary_and_source() {
		assertBinaryAndSource("A", "A.f", 2,
			"""
				public class A {
					public java.util.List<String> f;
				}""",
			"""
				public class A {
					public java.util.ArrayList<String> f;
				}"""
		);
	}

	@Client("java.util.List<String> l = new A().f;")
	@Test
	void generic_erasure_change_final_binary_only() {
		assertBinaryOnly("A", "A.f", 2,
			"""
				public class A {
					public final java.util.List<String> f = null;
				}""",
			"""
				public class A {
					public final java.util.ArrayList<String> f = null;
				}"""
		);
	}

	@Client("java.util.List<String> l = new A().f;")
	@Test
	void raw_subtype_to_parameterized_supertype_final_binary_only() {
		assertBinaryOnly("A", "A.f", 2,
			"""
				public class A {
					public final java.util.List<String> f = null;
				}""",
			"""
				public class A {
					public final java.util.ArrayList f = null;
				}"""
		);
	}

	@Client("java.util.List l = new A().f;")
	@Test
	void raw_to_parameterized_non_final_no_break() {
		var v1 = """
			public class A {
				public java.util.List f;
			}""";
		var v2 = """
			public class A {
				public java.util.List<String> f;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("java.util.List<String> l = new A().f;")
	@Test
	void parameterized_to_raw_non_final_no_break() {
		var v1 = """
			public class A {
				public java.util.List<String> f;
			}""";
		var v2 = """
			public class A {
				public java.util.List f;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A<CharSequence, String>().f = new StringBuilder();")
	@Test
	void type_parameter_bound_refinement_non_final_source_only() {
		assertSourceOnly("A", "A.f", 2,
			"""
				public class A<T, U extends T> {
					public T f;
				}""",
			"""
				public class A<T, U extends T> {
					public U f;
				}"""
		);
	}

	@Client("CharSequence cs = new A<CharSequence, String>().f;")
	@Test
	void type_parameter_bound_refinement_final_no_break() {
		var v1 = """
			public class A<T, U extends T> {
				public final T f = null;
			}""";
		var v2 = """
			public class A<T, U extends T> {
				public final U f = null;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("Integer i = new A<Integer, String>().f;")
	@Test
	void unrelated_type_parameters_source_only() {
		assertSourceOnly("A", "A.f", 2,
			"""
				public class A<T, U> {
					public T f;
				}""",
			"""
				public class A<T, U> {
					public U f;
				}"""
		);
	}

	@Client("int i = I.f;")
	@Test
	void interface_constant_type_change_source_only() {
		assertSourceOnly("I", "I.f", 2,
			"""
				public interface I {
					int f = 0;
				}""",
			"""
				public interface I {
					double f = 0;
				}"""
		);
	}

	@Client("Double d = I.f;")
	@Test
	void interface_constant_narrowing_type_change_source_only() {
		assertSourceOnly("I", "I.f", 2,
			"""
				public interface I {
					double f = 0;
				}""",
			"""
				public interface I {
					int f = 0;
				}"""
		);
	}

	@Client("int i = A.f;")
	@Test
	void class_constant_type_change_source_only() {
		assertSourceOnly("A", "A.f", 2,
			"""
				public class A {
					public static final int f = 0;
				}""",
			"""
				public class A {
					public static final double f = 0;
				}"""
		);
	}

	@Client("int i = A.f;")
	@Test
	void non_constant_static_final_type_change_binary_and_source() {
		assertBinaryAndSource("A", "A.f", 2,
			"""
				public class A {
					public static final int f = Integer.getInteger("x", 0);
				}""",
			"""
				public class A {
					public static final double f = Integer.getInteger("x", 0);
				}"""
		);
	}
}
