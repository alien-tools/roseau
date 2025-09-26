package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

/*
 * To be refined once we distinguish binary vs source compatibility
 */
class FieldTypeChangedTest {
	@Test
	void to_unknown() {
		var v1 = """
			public class A {
				public A f;
			}""";
		var v2 = """
			public class A {
				public Unknown f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void from_unknown() {
		var v1 = """
			public class A {
				public Unknown f;
			}""";
		var v2 = """
			public class A {
				public A f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("int i = new A().f;")
	@Test
	void boxing() {
		var v1 = """
			public class A {
				public int f;
			}""";
		var v2 = """
			public class A {
				public Integer f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("Integer i = new A().f;")
	@Test
	void unboxing() {
		var v1 = """
			public class A {
				public Integer f;
			}""";
		var v2 = """
			public class A {
				public int f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("int i = new A().f;")
	@Test
	void widening() {
		var v1 = """
			public class A {
				public int f;
			}""";
		var v2 = """
			public class A {
				public long f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("long l = new A().f;")
	@Test
	void narrowing() {
		var v1 = """
			public class A {
				public long f;
			}""";
		var v2 = """
			public class A {
				public int f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		new A().f = new java.io.InputStream() {
			@Override public int read() {
				return 0;
			}
		};""")
	@Test
	void subtype_jdk() {
		var v1 = """
			public class A {
				public java.io.InputStream f;
			}""";
		var v2 = """
			public class A {
				public java.io.FileInputStream f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.io.FileInputStream fis = new A().f;")
	@Test
	void supertype_jdk() {
		var v1 = """
			public class A {
				public java.io.FileInputStream f;
			}""";
		var v2 = """
			public class A {
				public java.io.InputStream f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().f = new I() {};")
	@Test
	void subtype_api() {
		var v1 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public I f;
			}""";
		var v2 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public J f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("J j = new A().f;")
	@Test
	void supertype_api() {
		var v1 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public J f;
			}""";
		var v2 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public I f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("I i = new A().f;")
	@Test
	void incompatible_api() {
		var v1 = """
			public interface I {}
			public interface J {}
			public class A {
				public I f;
			}""";
		var v2 = """
			public interface I {}
			public interface J {}
			public class A {
				public J f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("Integer i = new A<Integer, String>().f;")
	@Test
	void incompatible_type_parameter() {
		var v1 = """
			public class A<T, U> {
				public T f;
			}""";
		var v2 = """
			public class A<T, U> {
				public U f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A<java.util.HashMap, java.util.LinkedHashMap>().f = new java.util.HashMap<>();")
	@Test
	void subtype_type_parameter() {
		var v1 = """
			public class A<T, U extends T> {
				public T f;
			}""";
		var v2 = """
			public class A<T, U extends T> {
				public U f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.util.LinkedHashMap m = new A<java.util.HashMap, java.util.LinkedHashMap>().f;")
	@Test
	void supertype_type_parameter() {
		var v1 = """
			public class A<T, U extends T> {
				public U f;
			}""";
		var v2 = """
			public class A<T, U extends T> {
				public T f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.util.List<Integer> l = new A().f;")
	@Test
	void incompatible_generic() {
		var v1 = """
			public class A {
				public java.util.List<Integer> f;
			}""";
		var v2 = """
			public class A {
				public java.util.List<String> f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.util.List<I> l = new A().f;")
	@Test
	void subtype_generic() {
		var v1 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public java.util.List<I> f;
			}""";
		var v2 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public java.util.List<J> f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.util.List<J> l = new A().f;")
	@Test
	void supertype_generic() {
		var v1 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public java.util.List<J> f;
			}""";
		var v2 = """
			public interface I {}
			public interface J extends I {}
			public class A {
				public java.util.List<I> f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("int[] a = new A().f;")
	@Test
	void incompatible_array() {
		var v1 = """
			public class A {
				public int[] f;
			}""";
		var v2 = """
			public class A {
				public String[] f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.io.InputStream[] a = new A().f;")
	@Test
	void subtype_array() {
		var v1 = """
			public class A {
				public java.io.InputStream[] f;
			}""";
		var v2 = """
			public class A {
				public java.io.FileInputStream[] f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("java.io.FileInputStream[] a = new A().f;")
	@Test
	void supertype_array() {
		var v1 = """
			public class A {
				public java.io.FileInputStream[] f;
			}""";
		var v2 = """
			public class A {
				public java.io.InputStream[] f;
			}""";

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("""
		int i = I.f;
		Object o = new C().f;
		String s = new X().f;""")
	@Test
	void subtype_shadowing() {
		var v1 = """
			public interface I {
				public int f = 0;
			}
			public class C {
				public Object f = null;
			}
			public class X extends C implements I {
				public String f = "";
			}""";
		var v2 = """
			public interface I {
				public int f = 0;
			}
			public class C {
				public Object f = null;
			}
			public class X extends C implements I {
				public String f = "";
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
