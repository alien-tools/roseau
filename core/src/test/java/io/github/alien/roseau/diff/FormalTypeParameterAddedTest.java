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

	// §15.27.3: a lambda expression cannot implement a generic method
	@Client("I i = () -> {};")
	@Test
	void interface_method_first_param_added() {
		var v1 = """
			public interface I {
				void m();
			}""";
		var v2 = """
			public interface I {
				<T> void m();
			}""";

		assertBC("I", "I.m()", BreakingChangeKind.FORMAL_TYPE_PARAMETER_ADDED, 2, buildDiff(v1, v2));
	}

	// A lambda expression can implement m through a subinterface that implements n
	@Client("""
		interface J extends I { default void n() {} }
		J j = () -> {};""")
	@Test
	void interface_method_first_param_added_next_to_another_abstract_method() {
		var v1 = """
			public interface I {
				void m();
				void n();
			}""";
		var v2 = """
			public interface I {
				<T> void m();
				void n();
			}""";

		assertBC("I", "I.m()", BreakingChangeKind.FORMAL_TYPE_PARAMETER_ADDED, 2, buildDiff(v1, v2));
	}

	@Client("new I() {}.m();")
	@Test
	void interface_default_method_first_param_added() {
		var v1 = """
			public interface I {
				default void m() {}
			}""";
		var v2 = """
			public interface I {
				default <T> void m() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("new A() { @Override public void m() {} };")
	@Test
	void abstract_method_first_param_added() {
		var v1 = """
			public abstract class A {
				public abstract void m();
			}""";
		var v2 = """
			public abstract class A {
				public abstract <T> void m();
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
