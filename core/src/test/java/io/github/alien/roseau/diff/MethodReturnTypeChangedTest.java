package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

/**
 * Will need some refinement once we distinguish source- vs. binary-compatibility. Many cares are already written, but
 * they all map to a simple METHOD_RETURN_TYPE_CHANGED.
 */
class MethodReturnTypeChangedTest {
	@Client("new A().m();")
	@Test
	void void_to_non_void() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public int m() { return 0; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("int i = new A().m();")
	@Test
	void non_void_to_void() {
		var v1 = """
			public class A {
				public int m() { return 0; }
			}""";
		var v2 = """
			public class A {
				public void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("int i = new A().m();")
	@Test
	void boxing() {
		var v1 = """
			public class A {
				public int m() { return 0; }
			}""";
		var v2 = """
			public class A {
				public Integer m() { return 0; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("Integer i = new A().m();")
	@Test
	void unboxing() {
		var v1 = """
			public class A {
				public Integer m() { return 0; }
			}""";
		var v2 = """
			public class A {
				public int m() { return 0; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("int i = new A().m();")
	@Test
	void widening() {
		var v1 = """
			public class A {
				public int m() { return 0; }
			}""";
		var v2 = """
			public class A {
				public long m() { return 0L; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("long l = new A().m();")
	@Test
	void narrowing() {
		var v1 = """
			public class A {
				public long m() { return 0L; }
			}""";
		var v2 = """
			public class A {
				public int m() { return 0; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.io.InputStream is = new A().m();")
	@Test
	void subtype_jdk() {
		var v1 = """
			public class A {
				public java.io.InputStream m() { return null; }
			}""";
		var v2 = """
			public class A {
				public java.io.FileInputStream m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.io.FileInputStream fis = new A().m();")
	@Test
	void supertype_jdk() {
		var v1 = """
			public class A {
				public java.io.FileInputStream m() { return null; }
			}""";
		var v2 = """
			public class A {
				public java.io.InputStream m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("I i = new A().m();")
	@Test
	void subtype_api() {
		var v1 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public I m() { return null; }
			}""";
		var v2 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public J m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("J j = new A().m();")
	@Test
	void supertype_api() {
		var v1 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public J m() { return null; }
			}""";
		var v2 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public I m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("I i = new A().m();")
	@Test
	void incompatible_api() {
		var v1 = """
			public interface I {}
			public interface J {}
			public class A {
				public I m() { return null; }
			}""";
		var v2 = """
			public interface I {}
			public interface J {}
			public class A {
				public J m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("Integer i = new A<Integer, String>().m();")
	@Test
	void incompatible_type_parameter() {
		var v1 = """
			public class A<T, U> {
				public T m() { return null; }
			}""";
		var v2 = """
			public class A<T, U> {
				public U m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A<CharSequence, String>() {
			@Override public CharSequence m() { return null; }
		};""")
	@Test
	void subtype_type_parameter() {
		var v1 = """
			public class A<T, U extends T> {
				public T m() { return null; }
			}""";
		var v2 = """
			public class A<T, U extends T> {
				public U m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("String s = new A<CharSequence, String>().m();")
	@Test
	void supertype_type_parameter() {
		var v1 = """
			public class A<T, U extends T> {
				public U m() { return null; }
			}""";
		var v2 = """
			public class A<T, U extends T> {
				public T m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.util.List<Integer> l = new A().m();")
	@Test
	void incompatible_generic() {
		var v1 = """
			public class A {
				public java.util.List<Integer> m() { return null; }
			}""";
		var v2 = """
			public class A {
				public java.util.List<String> m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.util.List<I> l = new A().m();")
	@Test
	void subtype_generic() {
		var v1 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public java.util.List<I> m() { return null; }
			}""";
		var v2 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public java.util.List<J> m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.util.List<J> l = new A().m();")
	@Test
	void supertype_generic() {
		var v1 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public java.util.List<J> m() { return null; }
			}""";
		var v2 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public java.util.List<I> m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("int[] a = new A().m();")
	@Test
	void incompatible_array() {
		var v1 = """
			public class A {
				public int[] m() { return new int[] { 0 }; }
			}""";
		var v2 = """
			public class A {
				public String[] m() { return new String[] { "" }; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.io.InputStream[] a = new A().m();")
	@Test
	void subtype_array() {
		var v1 = """
			public class A {
				public java.io.InputStream[] m() { return new java.io.InputStream[]{ null }; }
			}""";
		var v2 = """
			public class A {
				public java.io.FileInputStream[] m() { return new java.io.FileInputStream[] { null }; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.io.FileInputStream[] a = new A().m();")
	@Test
	void supertype_array() {
		var v1 = """
			public class A {
				public java.io.FileInputStream[] m() { return new java.io.FileInputStream[] { null }; }
			}""";
		var v2 = """
			public class A {
				public java.io.InputStream[] m() { return new java.io.InputStream[] { null }; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("@A(0) class X {}")
	@Test
	void annotation_interface_method() {
		var v1 = """
			public @interface A {
				int value();
			}""";
		var v2 = """
			public @interface A {
				String value();
			}""";

		assertBC("A", "A.value()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("int i = new B().m();")
	@Test
	void changed_in_super_type() {
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
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2),
			bc("B", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2));
	}

	@Client("B.m();")
	@Test
	void inherited_static_method_changed() {
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

		assertBC("B", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Disabled("Not even binary-breaking cause of erasure")
	@Client("""
		new A<CharSequence, String>() {
			// Cannot @Override
		};
		CharSequence cs = new A<CharSequence, String>().m();""")
	@Test
	void subtype_type_parameter_final() {
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

	// Binary-compatible iff:
	//   - It's erased (field, param, return, array component) and T <: Object, incompatible otherwise
	//   - It's suppressed (type argument e.g. List<T>)
	// Always source-incompatible
	// Needs to check in which case 'final' makes it non-breaking: when implementers are breaking

	@Client("""
		new A<String>() {
			@Override public java.util.List<String> m() { return super.m(); }
		};
		java.util.List<String> l = new A<String>().m();""")
	@Test
	void generic_bounded_to_object() {
		var v1 = """
			public class A<T extends String> {
				public java.util.List<T> m() { return null; }
			}""";
		var v2 = """
			public class A<T extends String> {
				public java.util.List<Object> m() { return null; }
			}""";

		// Only source-breaking
		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public java.util.List<Object> m() { return super.m(); }
		};
		java.util.List<Object> l = new A<String>().m();""")
	@Test
	void generic_object_to_bounded() {
		var v1 = """
			public class A<T extends String> {
				public java.util.List<Object> m() { return null; }
			}""";
		var v2 = """
			public class A<T extends String> {
				public java.util.List<T> m() { return null; }
			}""";

		// Only source-breaking
		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public java.util.List<String> m() { return super.m(); }
		};
		java.util.List<String> l = new A<String>().m();""")
	@Test
	void generic_unbounded_to_object() {
		var v1 = """
			public class A<T> {
				public java.util.List<T> m() { return null; }
			}""";
		var v2 = """
			public class A<T> {
				public java.util.List<Object> m() { return null; }
			}""";

		// Only source-breaking
		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public java.util.List<Object> m() { return super.m(); }
		};
		java.util.List<Object> l = new A<String>().m();""")
	@Test
	void generic_object_to_unbounded() {
		var v1 = """
			public class A<T> {
				public java.util.List<Object> m() { return null; }
			}""";
		var v2 = """
			public class A<T> {
				public java.util.List<T> m() { return null; }
			}""";

		// Only source-breaking
		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public String m() { return super.m(); }
		};
		String s = new A<String>().m();""")
	@Test
	void unbounded_to_object() {
		var v1 = """
			public class A<T> {
				public T m() { return null; }
			}""";
		var v2 = """
			public class A<T> {
				public Object m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public Object m() { return super.m(); }
		};
		Object s = new A<String>().m();""")
	@Test
	void object_to_unbounded() {
		var v1 = """
			public class A<T> {
				public Object m() { return null; }
			}""";
		var v2 = """
			public class A<T> {
				public T m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public String m() { return super.m(); }
		};
		String s = new A<String>().m();""")
	@Test
	void bounded_to_object() {
		var v1 = """
			public class A<T extends String> {
				public T m() { return null; }
			}""";
		var v2 = """
			public class A<T extends String> {
				public Object m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public Object m() { return super.m(); }
		};
		Object s = new A<String>().m();""")
	@Test
	void object_to_bounded() {
		var v1 = """
			public class A<T extends String> {
				public Object m() { return null; }
			}""";
		var v2 = """
			public class A<T extends String> {
				public T m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public String m() { return super.m(); }
		};
		String s = new A<String>().m();""")
	@Test
	void bounded_to_matching_type() {
		var v1 = """
			public class A<T extends String> {
				public T m() { return null; }
			}""";
		var v2 = """
			public class A<T extends String> {
				public String m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public String m() { return super.m(); }
		};
		String s = new A<String>().m();""")
	@Test
	void type_to_matching_bounded() {
		var v1 = """
			public class A<T extends String> {
				public String m() { return null; }
			}""";
		var v2 = """
			public class A<T extends String> {
				public T m() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public String[] m() { return super.m(); }
		};
		String[] a = new A<String>().m();""")
	@Test
	void unbounded_array_to_object() {
		var v1 = """
			public class A<T> {
				public T[] m() { return null; }
			}""";
		var v2 = """
			public class A<T> {
				public Object[] m() { return new Object[] { null }; }
			}""";

		// Source-breaking, not binary-breaking
		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public Object[] m() { return super.m(); }
		};
		Object[] a = new A<String>().m();""")
	@Test
	void object_array_to_unbounded() {
		var v1 = """
			public class A<T> {
				public Object[] m() { return new Object[] { null }; }
			}""";
		var v2 = """
			public class A<T> {
				public T[] m() { return null; }
			}""";

		// Source-breaking, not binary-breaking
		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public String[] m() { return super.m(); }
		};
		String[] a = new A<String>().m();""")
	@Test
	void bounded_array_to_object() {
		var v1 = """
			public class A<T extends String> {
				public T[] m() { return null; }
			}""";
		var v2 = """
			public class A<T extends String> {
				public Object[] m() { return new Object[] { null }; }
			}""";

		// Both breaking
		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A<String>() {
			@Override public Object[] m() { return super.m(); }
		};
		Object[] a = new A<String>().m();""")
	@Test
	void object_array_to_bounded() {
		var v1 = """
			public class A<T extends String> {
				public Object[] m() { return null; }
			}""";
		var v2 = """
			public class A<T extends String> {
				public T[] m() { return null; }
			}""";

		// Both breaking
		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void to_unknown() {
		var v1 = """
			public class A {
				public A m() { return null; }
			}""";
		var v2 = """
			public class A {
				public Unknown m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void from_unknown() {
		var v1 = """
			public class A {
				public Unknown m() { return null; }
			}""";
		var v2 = """
			public class A {
				public A m() { return null; }
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}
}
