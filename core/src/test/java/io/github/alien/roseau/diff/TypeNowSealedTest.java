package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class TypeNowSealedTest {
	@Client("new A(){};")
	@Test
	void class_now_sealed() {
		var v1 = """
			public class A {
				public void m() {}
			}""";
		var v2 = """
			public sealed class A permits B {
				public void m() {}
			}
			final class B extends A {}""";

		assertBC("A", "A", BreakingChangeKind.TYPE_NOW_SEALED, 1, buildDiff(v1, v2));
	}

	@Client("class C implements I {}")
	@Test
	void interface_now_sealed() {
		var v1 = "public interface I {}";
		var v2 = """
			public sealed interface I permits X {}
			final class X implements I {}""";

		assertBC("I", "I", BreakingChangeKind.TYPE_NOW_SEALED, 1, buildDiff(v1, v2));
	}

	@Client("class C implements I { @Override public void m() {} }")
	@Test
	void default_method_does_not_become_final_when_interface_becomes_sealed() {
		var v1 = """
			public interface I {
				default void m() {}
			}""";
		var v2 = """
			public sealed interface I permits X {
				default void m() {}
			}
			final class X implements I {}""";

		assertBC("I", "I", BreakingChangeKind.TYPE_NOW_SEALED, 1, buildDiff(v1, v2));
	}

	@Client("class C implements I {}")
	@Test
	void default_method_now_abstract_while_interface_becomes_sealed() {
		var v1 = """
			public interface I {
				default void m() {}
			}""";
		var v2 = """
			public sealed interface I permits X {
				void m();
			}
			final class X implements I {
				public void m() {}
			}""";

		assertBC("I", "I", BreakingChangeKind.TYPE_NOW_SEALED, 1, buildDiff(v1, v2));
	}

	@Client("class C implements I {}")
	@Test
	void abstract_method_added_while_interface_becomes_sealed() {
		var v1 = "public interface I {}";
		var v2 = """
			public sealed interface I permits X {
				void m();
			}
			final class X implements I {
				public void m() {}
			}""";

		assertBC("I", "I", BreakingChangeKind.TYPE_NOW_SEALED, 1, buildDiff(v1, v2));
	}

	@Client("new A(){};")
	@Test
	void concrete_method_now_abstract_while_class_becomes_sealed() {
		var v1 = """
			public abstract class A {
				public void m() {}
			}""";
		var v2 = """
			public abstract sealed class A permits B {
				public abstract void m();
			}
			final class B extends A {
				public void m() {}
			}""";

		assertBC("A", "A", BreakingChangeKind.TYPE_NOW_SEALED, 1, buildDiff(v1, v2));
	}

	@Client("new A(){};")
	@Test
	void abstract_method_added_while_class_becomes_sealed() {
		var v1 = "public abstract class A {}";
		var v2 = """
			public abstract sealed class A permits B {
				public abstract void m();
			}
			final class B extends A {
				public void m() {}
			}""";

		assertBC("A", "A", BreakingChangeKind.TYPE_NOW_SEALED, 1, buildDiff(v1, v2));
	}

	@Client("A a = null;")
	@Test
	void final_class_now_sealed() {
		var v1 = "public final class A {}";
		var v2 = """
			public sealed class A permits B {}
			final class B extends A {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("// Cannot subtype A in v1")
	@Test
	void already_unsubclassable_class_now_sealed() {
		var v1 = """
			public class A {
				private A() {}
			}""";
		var v2 = """
			public sealed class A permits A.B {
				private A() {}
				static final class B extends A {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("// Cannot manipulate A")
	@Test
	void unsubclassable_class_with_extensible_subclass_now_sealed() {
		var v1 = """
			public class A {
				A() {}
			}
			public class B extends A {
				public B() {}
			}""";
		var v2 = """
			public sealed class A permits B {
				A() {}
			}
			public non-sealed class B extends A {
				public B() {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
