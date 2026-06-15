package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class SealedHierarchyExtensibilityTest {
	@Client("""
		class C extends B { @Override public void m() {} }
		new C().m();""")
	@Test
	void method_now_final_in_sealed_class_with_non_sealed_subclass() {
		var v1 = """
			public sealed class A permits B {
				public void m() {}
			}
			public non-sealed class B extends A {}""";
		var v2 = """
			public sealed class A permits B {
				public final void m() {}
			}
			public non-sealed class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_NOW_FINAL, 2),
			bc("B", "A.m()", BreakingChangeKind.METHOD_NOW_FINAL, 2));
	}

	@Client("// Cannot subtype package-private B from another package")
	@Test
	void method_now_final_in_sealed_class_with_internal_non_sealed_subclass() {
		var v1 = """
			public sealed class A permits B {
				public void m() {}
			}
			non-sealed class B extends A {}""";
		var v2 = """
			public sealed class A permits B {
				public final void m() {}
			}
			non-sealed class B extends A {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		class D extends C { @Override public void m() {} }
		new D().m();""")
	@Test
	void method_now_final_in_sealed_class_with_exported_non_sealed_descendant() {
		var v1 = """
			public sealed class A permits B {
				public void m() {}
			}
			sealed class B extends A permits C {}
			public non-sealed class C extends B {}""";
		var v2 = """
			public sealed class A permits B {
				public final void m() {}
			}
			sealed class B extends A permits C {}
			public non-sealed class C extends B {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_NOW_FINAL, 2),
			bc("C", "A.m()", BreakingChangeKind.METHOD_NOW_FINAL, 2));
	}

	@Client("""
		class C extends B { @Override public void m() {} }
		new C().m();
		new B().m();""")
	@Test
	void method_now_static_in_sealed_class_with_non_sealed_subclass() {
		var v1 = """
			public sealed class A permits B {
				public void m() {}
			}
			public non-sealed class B extends A {}""";
		var v2 = """
			public sealed class A permits B {
				public static void m() {}
			}
			public non-sealed class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_NOW_STATIC, 2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_OVERRIDABLE_NOW_STATIC, 2),
			bc("B", "A.m()", BreakingChangeKind.METHOD_NOW_STATIC, 2),
			bc("B", "A.m()", BreakingChangeKind.METHOD_OVERRIDABLE_NOW_STATIC, 2));
	}

	@Client("""
		class C extends B {}
		new C().m();""")
	@Test
	void method_now_abstract_in_sealed_class_with_non_sealed_subclass() {
		var v1 = """
			public abstract sealed class A permits B {
				public void m() {}
			}
			public abstract non-sealed class B extends A {}""";
		var v2 = """
			public abstract sealed class A permits B {
				public abstract void m();
			}
			public abstract non-sealed class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2),
			bc("B", "A.m()", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2));
	}

	@Client("""
		class C implements J {}
		new C().m();""")
	@Test
	void default_method_now_abstract_in_sealed_interface_with_non_sealed_subinterface() {
		var v1 = """
			public sealed interface I permits J {
				default void m() {}
			}
			public non-sealed interface J extends I {}""";
		var v2 = """
			public sealed interface I permits J {
				void m();
			}
			public non-sealed interface J extends I {}""";

		assertBCs(buildDiff(v1, v2),
			bc("I", "I.m()", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2),
			bc("J", "I.m()", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2));
	}

	@Client("class C implements J {}")
	@Test
	void abstract_method_added_to_sealed_interface_with_non_sealed_subinterface() {
		var v1 = """
			public sealed interface I permits J {}
			public non-sealed interface J extends I {}""";
		var v2 = """
			public sealed interface I permits J {
				void m();
			}
			public non-sealed interface J extends I {}""";

		assertBCs(buildDiff(v1, v2),
			bc("I", "I", BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, 1),
			bc("J", "J", BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, 1));
	}

	@Client("""
		class C extends B {
			@Override public java.util.List<?> m() { return java.util.List.of(); }
		}""")
	@Test
	void return_type_changed_in_sealed_class_with_non_sealed_subclass() {
		var v1 = """
			public sealed class A permits B {
				public java.util.List<?> m() { return java.util.List.of(); }
			}
			public non-sealed class B extends A {}""";
		var v2 = """
			public sealed class A permits B {
				public java.util.List<String> m() { return java.util.List.of(); }
			}
			public non-sealed class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2),
			bc("B", "A.m()", BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("""
		class C extends B {
			@Override public void m(java.util.List<String> l) {}
		}""")
	@Test
	void parameter_generics_changed_in_sealed_class_with_non_sealed_subclass() {
		var v1 = """
			public sealed class A permits B {
				public void m(java.util.List<String> l) {}
			}
			public non-sealed class B extends A {}""";
		var v2 = """
			public sealed class A permits B {
				public void m(java.util.List<?> l) {}
			}
			public non-sealed class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m(java.util.List<java.lang.String>)",
				BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2),
			bc("B", "A.m(java.util.List<java.lang.String>)",
				BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED, 2));
	}

	@Client("""
		class C extends B {
			@Override public void m() throws java.io.IOException {}
		}""")
	@Test
	void thrown_exception_narrowed_in_sealed_class_with_non_sealed_subclass() {
		var v1 = """
			public sealed class A permits B {
				public void m() throws java.io.IOException {}
			}
			public non-sealed class B extends A {}""";
		var v2 = """
			public sealed class A permits B {
				public void m() throws java.io.FileNotFoundException {}
			}
			public non-sealed class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m()", BreakingChangeKind.EXECUTABLE_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2),
			bc("B", "A.m()", BreakingChangeKind.EXECUTABLE_NO_LONGER_THROWS_CHECKED_EXCEPTION, 2));
	}

	@Client("""
		class C extends B {
			@Override public <T extends CharSequence & java.io.Serializable> void m(T t) {}
		}""")
	@Test
	void method_type_parameter_changed_in_sealed_class_with_non_sealed_subclass() {
		var v1 = """
			public sealed class A permits B {
				public <T extends CharSequence & java.io.Serializable> void m(T t) {}
			}
			public non-sealed class B extends A {}""";
		var v2 = """
			public sealed class A permits B {
				public <T extends CharSequence> void m(T t) {}
			}
			public non-sealed class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.m(T)", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 2),
			bc("B", "A.m(T)", BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, 2));
	}

	@Client("""
		class C extends B {
			void use() { p(); }
		}
		new C().use();""")
	@Test
	void protected_method_removed_from_sealed_class_with_non_sealed_subclass() {
		var v1 = """
			public sealed class A permits B {
				protected void p() {}
			}
			public non-sealed class B extends A {}""";
		var v2 = """
			public sealed class A permits B {}
			public non-sealed class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.p()", BreakingChangeKind.EXECUTABLE_REMOVED, 2),
			bc("B", "A.p()", BreakingChangeKind.EXECUTABLE_REMOVED, 2));
	}

	@Client("""
		class C extends B {
			int use() { return f; }
		}
		new C().use();""")
	@Test
	void protected_field_removed_from_sealed_class_with_non_sealed_subclass() {
		var v1 = """
			public sealed class A permits B {
				protected int f;
			}
			public non-sealed class B extends A {}""";
		var v2 = """
			public sealed class A permits B {}
			public non-sealed class B extends A {}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.f", BreakingChangeKind.FIELD_REMOVED, 2),
			bc("B", "A.f", BreakingChangeKind.FIELD_REMOVED, 2));
	}
}
