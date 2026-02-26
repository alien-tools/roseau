package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class MethodReturnTypeChangedTest {
	private static void assertBinaryOnly(String type, String method, int line, String v1, String v2) {
		assertBCs(buildDiff(v1, v2), bc(type, method, BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, line));
	}

	private static void assertSourceOnly(String type, String method, int line, String v1, String v2) {
		assertBCs(buildDiff(v1, v2), bc(type, method, BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, line));
	}

	private static void assertBinaryAndSource(String type, String method, int line, String v1, String v2) {
		assertBCs(buildDiff(v1, v2),
			bc(type, method, BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, line),
			bc(type, method, BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, line));
	}

	@Client("new A().m();")
	@Test
	void void_to_non_void_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public void m() {}
				}""",
			"""
				public class A {
					public int m() { return 0; }
				}"""
		);
	}

	@Client("int i = new A().m();")
	@Test
	void non_void_to_void_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public int m() { return 0; }
				}""",
			"""
				public class A {
					public void m() {}
				}"""
		);
	}

	@Client("int i = new A().m();")
	@Test
	void boxing_non_final_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public int m() { return 0; }
				}""",
			"""
				public class A {
					public Integer m() { return 0; }
				}"""
		);
	}

	@Client("int i = new A().m();")
	@Test
	void boxing_final_binary_only() {
		assertBinaryOnly("A", "A.m()", 2,
			"""
				public class A {
					public final int m() { return 0; }
				}""",
			"""
				public class A {
					public final Integer m() { return 0; }
				}"""
		);
	}

	@Client("Integer i = new A().m();")
	@Test
	void unboxing_non_final_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public Integer m() { return 0; }
				}""",
			"""
				public class A {
					public int m() { return 0; }
				}"""
		);
	}

	@Client("int i = new A().m();")
	@Test
	void primitive_widening_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public int m() { return 0; }
				}""",
			"""
				public class A {
					public long m() { return 0L; }
				}"""
		);
	}

	@Client("long l = new A().m();")
	@Test
	void primitive_narrowing_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public long m() { return 0L; }
				}""",
			"""
				public class A {
					public int m() { return 0; }
				}"""
		);
	}

	@Client("java.io.InputStream is = new A().m();")
	@Test
	void reference_subtype_non_final_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public java.io.InputStream m() { return null; }
				}""",
			"""
				public class A {
					public java.io.FileInputStream m() { return null; }
				}"""
		);
	}

	@Client("java.io.InputStream is = new A().m();")
	@Test
	void reference_subtype_final_binary_only() {
		assertBinaryOnly("A", "A.m()", 2,
			"""
				public class A {
					public final java.io.InputStream m() { return null; }
				}""",
			"""
				public class A {
					public final java.io.FileInputStream m() { return null; }
				}"""
		);
	}

	@Client("java.io.FileInputStream fis = new A().m();")
	@Test
	void reference_supertype_non_final_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public java.io.FileInputStream m() { return null; }
				}""",
			"""
				public class A {
					public java.io.InputStream m() { return null; }
				}"""
		);
	}

	@Client("Integer i = new A<Integer, String>().m();")
	@Test
	void unrelated_type_parameters_source_only() {
		assertSourceOnly("A", "A.m()", 2,
			"""
				public class A<T, U> {
					public T m() { return null; }
				}""",
			"""
				public class A<T, U> {
					public U m() { return null; }
				}"""
		);
	}

	@Client("new A<CharSequence, String>().m();")
	@Test
	void subtype_type_parameter_non_final_source_only() {
		assertSourceOnly("A", "A.m()", 2,
			"""
				public class A<T, U extends T> {
					public T m() { return null; }
				}""",
			"""
				public class A<T, U extends T> {
					public U m() { return null; }
				}"""
		);
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
		assertSourceOnly("A", "A.m()", 2,
			"""
				public class A<T, U extends T> {
					public U m() { return null; }
				}""",
			"""
				public class A<T, U extends T> {
					public T m() { return null; }
				}"""
		);
	}

	@Client("java.util.List<Integer> l = new A().m();")
	@Test
	void invariant_generic_argument_change_source_only() {
		assertSourceOnly("A", "A.m()", 2,
			"""
				public class A {
					public java.util.List<Integer> m() { return null; }
				}""",
			"""
				public class A {
					public java.util.List<String> m() { return null; }
				}"""
		);
	}

	@Client("java.util.List<String> l = new A().m();")
	@Test
	void generic_erasure_change_final_binary_only() {
		assertBinaryOnly("A", "A.m()", 2,
			"""
				public class A {
					public final java.util.List<String> m() { return null; }
				}""",
			"""
				public class A {
					public final java.util.ArrayList<String> m() { return null; }
				}"""
		);
	}

	@Client("int[] a = new A().m();")
	@Test
	void array_incompatible_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public int[] m() { return new int[] { 0 }; }
				}""",
			"""
				public class A {
					public String[] m() { return new String[] { \"\" }; }
				}"""
		);
	}

	@Client("java.io.InputStream[] a = new A().m();")
	@Test
	void array_subtype_non_final_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public java.io.InputStream[] m() { return new java.io.InputStream[] { null }; }
				}""",
			"""
				public class A {
					public java.io.FileInputStream[] m() { return new java.io.FileInputStream[] { null }; }
				}"""
		);
	}

	@Client("java.io.FileInputStream[] a = new A().m();")
	@Test
	void array_supertype_non_final_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public java.io.FileInputStream[] m() { return new java.io.FileInputStream[] { null }; }
				}""",
			"""
				public class A {
					public java.io.InputStream[] m() { return new java.io.InputStream[] { null }; }
				}"""
		);
	}

	@Client("@A(0) class X {}")
	@Test
	void annotation_interface_method_binary_and_source() {
		assertBinaryAndSource("A", "A.value()", 2,
			"""
				public @interface A {
					int value();
				}""",
			"""
				public @interface A {
					String value();
				}"""
		);
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
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public A m() { return null; }
				}""",
			"""
				public class A {
					public Unknown m() { return null; }
				}"""
		);
	}

	@Test
	void from_unknown_binary_and_source() {
		assertBinaryAndSource("A", "A.m()", 2,
			"""
				public class A {
					public Unknown m() { return null; }
				}""",
			"""
				public class A {
					public A m() { return null; }
				}"""
		);
	}
}
