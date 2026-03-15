package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class FormalTypeParameterAddedTest {
	@Client("A a;")
	@Test
	void class_first_param_added() {
		var v1 = "public class A {}";
		var v2 = "public class A<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A a;")
	@Test
	void class_two_params_added_to_non_generic() {
		var v1 = "public class A {}";
		var v2 = "public class A<T, U> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A a;")
	@Test
	void class_bounded_param_added_to_non_generic() {
		var v1 = "public class A {}";
		var v2 = "public class A<T extends Number> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String> a;")
	@Test
	void class_second_param_added() {
		var v1 = "public class A<T> {}";
		var v2 = "public class A<T, U> {}";

		assertBC("A", "A", BreakingChangeKind.FORMAL_TYPE_PARAMETER_ADDED, 1, buildDiff(v1, v2));
	}

	@Client("new A().m();")
	@Test
	void method_first_param_added() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public <T> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m();")
	@Test
	void method_two_params_added_to_non_generic() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public <T, U> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m();")
	@Test
	void method_bounded_param_added_to_non_generic() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public class A {
				public <T extends Number> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m(\"\");")
	@Test
	void method_first_param_added_and_used_as_parameter_type() {
		var v1 = """
			public class A {
				public void m(Object o) {}
			}""";
		var v2 = """
			public class A {
				public <T> void m(T t) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// Source-breaking: a subclass overriding m(List<Object>) gets a name clash with m(List<T>),
	// because List<Object> is not a subsignature of List<T> (erasure is raw List, not List<Object>).
	@Client("class B extends A { @Override public void m(java.util.List<Object> l) {} }")
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

		assertBC("A", "A.m(java.util.List<java.lang.Object>)", BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(1);")
	@Test
	void method_bounded_param_added_and_used_as_parameter_type() {
		var v1 = """
			public class A {
				public void m(Number n) {}
			}""";
		var v2 = """
			public class A {
				public <T extends Number> void m(T t) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().m(\"\");")
	@Test
	void type_first_param_added_and_used_as_method_parameter_type() {
		var v1 = """
			public class A {
				public void m(Object o) {}
			}""";
		var v2 = """
			public class A<T> {
				public void m(T t) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// Source-breaking: a subclass overriding m(List<Object>) gets a name clash with m(List<T>),
	// because List<Object> is not a subsignature of List<T> (erasure is raw List, not List<Object>).
	@Client("class B extends A { @Override public void m(java.util.List<Object> l) {} }")
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

		assertBC("A", "A.m(java.util.List<java.lang.Object>)", BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("new A().m(1);")
	@Test
	void type_bounded_param_added_and_used_as_method_parameter_type() {
		var v1 = """
			public class A {
				public void m(Number n) {}
			}""";
		var v2 = """
			public class A<T extends Number> {
				public void m(T t) {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A().<String>m();")
	@Test
	void method_second_param_added() {
		var v1 = """
			public class A {
				public <T> void m() {}
			}""";
		var v2 = """
			public class A {
				public <T, U> void m() {}
			}""";

		assertBC("A", "A.m()", BreakingChangeKind.FORMAL_TYPE_PARAMETER_ADDED, 2, buildDiff(v1, v2));
	}
}
