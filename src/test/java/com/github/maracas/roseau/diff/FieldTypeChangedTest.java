package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

/*
 * To be refined once we distinguish binary vs source compatibility
 */
class FieldTypeChangedTest {
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

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 4, buildDiff(v1, v2));
	}

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

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 4, buildDiff(v1, v2));
	}

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

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 4, buildDiff(v1, v2));
	}

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

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 4, buildDiff(v1, v2));
	}

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

		assertBC("A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 4, buildDiff(v1, v2));
	}

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
}
