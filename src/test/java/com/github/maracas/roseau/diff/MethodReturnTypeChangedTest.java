package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

/**
 * Will need some refinement once we distinguish source- vs. binary-compatibility.
 * Many cares are already written, but they all map to a simple METHOD_RETURN_TYPE_CHANGED.
 */
class MethodReturnTypeChangedTest {
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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Test
	void non_void_to_void() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public int m() { return 0; }
			}""";

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}

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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
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

		assertBC("A.m", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, 2, buildDiff(v1, v2));
	}
}
