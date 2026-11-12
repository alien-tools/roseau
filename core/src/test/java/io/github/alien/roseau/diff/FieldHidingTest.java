package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

/**
 * A field declaration hides the accessible fields of the same name in its supertypes, whatever its own visibility
 * (JLS §8.3): the hidden fields are not members of the hiding type, nor of its subtypes.
 */
class FieldHidingTest {
	private static final String A_V1 = """
		public class A {
			public static Object F;
		}
		""";
	private static final String A_V2 = """
		public class A {
			public static String F;
		}
		""";

	/**
	 * Changes the type of the hidden field {@code A.F}: only {@code A} is impacted
	 */
	private static void assertOnlyHiddenFieldImpacted(String subtypes) {
		assertOnlyHiddenFieldImpacted(buildDiff(A_V1 + subtypes, A_V2 + subtypes));
	}

	private static void assertOnlyHiddenFieldImpacted(List<BreakingChange> bcs) {
		assertBCs(bcs,
			bc("A", "A.F", BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.F", BreakingChangeKind.FIELD_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("Object o = ((A) new B()).F; // B.F has private access in B")
	@Test
	void hidden_by_private_field() {
		assertOnlyHiddenFieldImpacted("""
			public class B extends A {
				private static Object F;
			}""");
	}

	@Client("Object o = ((A) new B()).F; // B.F is not public in B")
	@Test
	void hidden_by_package_private_field() {
		assertOnlyHiddenFieldImpacted("""
			public class B extends A {
				static Object F;
			}""");
	}

	@Client("Object o = ((A) new B()).F; // B.F has protected access in B")
	@Test
	void hidden_by_protected_field_in_final_class() {
		assertOnlyHiddenFieldImpacted("""
			public final class B extends A {
				protected static Object F;
			}""");
	}

	@Client("new B() { Object o = F; };")
	@Test
	void hidden_by_protected_field() {
		assertOnlyHiddenFieldImpacted("""
			public class B extends A {
				protected static Object F;
			}""");
	}

	@Client("Object o = ((A) new C()).F; // C.F has private access in B")
	@Test
	void hidden_in_subtype_of_hiding_class() {
		assertOnlyHiddenFieldImpacted("""
			public class B extends A {
				private static Object F;
			}
			public class C extends B {}""");
	}

	@Client("Object o = ((A) new C()).F; // C.F has private access in B")
	@Test
	void hidden_in_subtype_of_package_private_hiding_class() {
		assertOnlyHiddenFieldImpacted("""
			class B extends A {
				private static Object F;
			}
			public class C extends B {}""");
	}

	@Client("Object o = ((A) new B()).f; // B.f has private access in B")
	@Test
	void instance_field_hidden_by_private_field() {
		var v1 = """
			public class A {
				public Object f;
			}
			public class B extends A {
				private int f;
			}""";
		var v2 = """
			public class A {
				public String f;
			}
			public class B extends A {
				private int f;
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.f", BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED, 2),
			bc("A", "A.f", BreakingChangeKind.FIELD_TYPE_CHANGED_INCOMPATIBLE, 2));
	}

	@Client("Object o = I.F; // B.F has private access in B")
	@Test
	void interface_constant_hidden_by_private_field() {
		var v1 = """
			public interface I {
				Object F = new Object();
			}
			public class B implements I {
				private static Object F;
			}""";
		var v2 = """
			public interface I {
				String F = new String();
			}
			public class B implements I {
				private static Object F;
			}""";

		assertBC("I", "I.F", BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED, 2, buildDiff(v1, v2));
	}

	@Client("Object o = A.F; // B.F has private access in B")
	@Test
	void hidden_field_removed() {
		var v1 = """
			public class A {
				public static Object F;
			}
			public class B extends A {
				private static Object F;
			}""";
		var v2 = """
			public class A {}
			public class B extends A {
				private static Object F;
			}""";

		assertBC("A", "A.F", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	// B does not inherit the private A.F, which does not hide I.F in B: B.F is I.F
	@Client("Object o = B.F;")
	@Test
	void private_field_does_not_hide_field_inherited_from_another_supertype() {
		var v1 = """
			public interface I {
				Object F = new Object();
			}
			public class A implements I {
				private static Object F;
			}
			public class B extends A implements I {}""";
		var v2 = """
			public interface I {
				String F = new String();
			}
			public class A implements I {
				private static Object F;
			}
			public class B extends A implements I {}""";

		assertBCs(buildDiff(v1, v2),
			bc("I", "I.F", BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED, 2),
			bc("B", "I.F", BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED, 2));
	}

	@Client("Object o = B.F;")
	@Test
	void private_hiding_field_added() {
		var v1 = A_V1 + """
			public class B extends A {}""";
		var v2 = A_V1 + """
			public class B extends A {
				private static Object F;
			}""";

		assertBC("B", "A.F", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("Object o = new B().f;")
	@Test
	void private_hiding_instance_field_added() {
		var v1 = """
			public class A {
				public Object f;
			}
			public class B extends A {}""";
		var v2 = """
			public class A {
				public Object f;
			}
			public class B extends A {
				private Object f;
			}""";

		assertBC("B", "A.f", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("Object o = C.F;")
	@Test
	void private_hiding_field_added_in_package_private_supertype() {
		var v1 = A_V1 + """
			class B extends A {}
			public class C extends B {}""";
		var v2 = A_V1 + """
			class B extends A {
				private static Object F;
			}
			public class C extends B {}""";

		assertBC("C", "A.F", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("Object o = B.F;")
	@Test
	void hiding_field_now_private() {
		var v1 = A_V1 + """
			public class B extends A {
				public static Object F;
			}""";
		var v2 = A_V1 + """
			public class B extends A {
				private static Object F;
			}""";

		assertBC("B", "B.F", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("Object o = ((A) new B()).F; // B.F has private access in B")
	@Test
	void private_hiding_field_removed() {
		var v1 = A_V1 + """
			public class B extends A {
				private static Object F;
			}""";
		var v2 = A_V1 + """
			public class B extends A {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("Object o = ((A) new B()).F; // B.F has private access in B")
	@Test
	void private_hiding_field_now_public() {
		var v1 = A_V1 + """
			public class B extends A {
				private static Object F;
			}""";
		var v2 = A_V1 + """
			public class B extends A {
				public static String F;
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	// A.F becomes inaccessible, but B still inherits I.F from its direct superinterface
	@Client("Object o = A.F;")
	@Test
	void private_hiding_field_added_in_supertype_with_another_inheritance_path() {
		var v1 = """
			public interface I {
				Object F = new Object();
			}
			public class A implements I {}
			public class B extends A implements I {}""";
		var v2 = """
			public interface I {
				Object F = new Object();
			}
			public class A implements I {
				private static Object F;
			}
			public class B extends A implements I {}""";

		assertBC("A", "I.F", BreakingChangeKind.FIELD_REMOVED, 2, buildDiff(v1, v2));
	}

	@Client("Object o = B.F;")
	@Test
	void protected_hiding_field_added() {
		var v1 = A_V1 + """
			public class B extends A {}""";
		var v2 = A_V1 + """
			public class B extends A {
				protected static Object F;
			}""";

		assertBC("B", "A.F", BreakingChangeKind.FIELD_NOW_PROTECTED, 2, buildDiff(v1, v2));
	}
}
