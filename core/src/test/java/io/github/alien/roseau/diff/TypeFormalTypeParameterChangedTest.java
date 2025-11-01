package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class TypeFormalTypeParameterChangedTest {
	@Client("A<String> a = new A<>();")
	@Test
	void param_renamed() {
		var v1 = "public class A<T> {}";
		var v2 = "public class A<U> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String, Integer> a = new A<>();")
	@Test
	void param_swapped() {
		var v1 = "public class A<T, U> {}";
		var v2 = "public class A<U, T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String, CharSequence> a = new A<>();")
	@Test
	void bounded_param_swapped() {
		var v1 = "public class A<T extends String, U extends CharSequence> {}";
		var v2 = "public class A<U extends CharSequence, T extends String> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<Object> a = new A<>();")
	@Test
	void bound_added() {
		var v1 = "public class A<T> {}";
		var v2 = "public class A<T extends String> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<Object> a = new A<>();")
	@Test
	void bound_object_added() {
		var v1 = "public class A<T> {}";
		var v2 = "public class A<T extends Object> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String> a = new A<>();")
	@Test
	void bound_removed() {
		var v1 = "public class A<T extends String> {}";
		var v2 = "public class A<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		abstract class X implements CharSequence, Runnable {}
		A<X> a = new A<>();
		""")
	@Test
	void second_bound_removed() {
		var v1 = "public class A<T extends CharSequence & Runnable> {}";
		var v2 = "public class A<T extends CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String, CharSequence> a = new A<>();")
	@Test
	void bound_param_removed() {
		var v1 = "public class A<T extends U, U> {}";
		var v2 = "public class A<T, U> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<Integer, Number> a = new A<>();")
	@Test
	void bound_param_added() {
		var v1 = "public class A<T, U> {}";
		var v2 = "public class A<T, U extends T> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<String> a = new A<>();")
	@Test
	void bound_modified_compatible() {
		var v1 = "public class A<T extends String> {}";
		var v2 = "public class A<T extends CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<StringBuilder> a = new A<>();")
	@Test
	void bound_modified_incompatible() {
		var v1 = "public class A<T extends CharSequence> {}";
		var v2 = "public class A<T extends String> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<String, StringBuilder, String> a = new A<>();")
	@Test
	void bound_modified_incompatible_param_1() {
		// Still breaking if client chooses a U that is not a subtype of T
		var v1 = "public class A<T extends String, U extends CharSequence, V extends T> {}";
		var v2 = "public class A<T extends String, U extends CharSequence, V extends U> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<String, CharSequence, CharSequence> a = new A<>();")
	@Test
	void bound_modified_incompatible_param_2() {
		var v1 = "public class A<T extends String, U extends CharSequence, V extends U> {}";
		var v2 = "public class A<T extends String, U extends CharSequence, V extends T> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<String> a = new A<>();")
	@Test
	void second_bound_added_compatible() {
		var v1 = "public class A<T extends String> {}";
		var v2 = "public class A<T extends String & CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<String> a = new A<>();")
	@Test
	void second_bound_added_incompatible() {
		var v1 = "public class A<T extends String> {}";
		var v2 = "public class A<T extends String & Runnable> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_changed_to_compatible_generic() {
		var v1 = "public class A<T extends java.util.List<? extends String>> {}";
		var v2 = "public class A<T extends java.util.List<? extends CharSequence>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<CharSequence>> a = new A<>();")
	@Test
	void bound_changed_to_incompatible_generic() {
		var v1 = "public class A<T extends java.util.List<? extends CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<? extends String>> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.ArrayList<String>> a = new A<>();")
	@Test
	void bound_changed_to_generic_supertype() {
		var v1 = "public class A<T extends java.util.ArrayList<? extends String>> {}";
		var v2 = "public class A<T extends java.util.List<? extends CharSequence>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_changed_to_generic_subtype() {
		var v1 = "public class A<T extends java.util.List<? extends String>> {}";
		var v2 = "public class A<T extends java.util.ArrayList<? extends CharSequence>> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<CharSequence>> a = new A<>();")
	@Test
	void bound_changed_to_compatible_generic_super() {
		var v1 = "public class A<T extends java.util.List<? super CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<? super String>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_changed_to_incompatible_generic_super() {
		var v1 = "public class A<T extends java.util.List<? super String>> {}";
		var v2 = "public class A<T extends java.util.List<? super CharSequence>> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.ArrayList<CharSequence>> a = new A<>();")
	@Test
	void bound_changed_to_incompatible_type_super() {
		var v1 = "public class A<T extends java.util.ArrayList<? super CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<? super String>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<CharSequence>> a = new A<>();")
	@Test
	void bound_changed_to_incompatible_subtype_super() {
		var v1 = "public class A<T extends java.util.List<? super CharSequence>> {}";
		var v2 = "public class A<T extends java.util.ArrayList<? super String>> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_changed_to_generic_wildcard_extends() {
		var v1 = "public class A<T extends java.util.List<? extends CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<?>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<Object>> a = new A<>();")
	@Test
	void bound_changed_from_generic_wildcard_extends() {
		var v1 = "public class A<T extends java.util.List<?>> {}";
		var v2 = "public class A<T extends java.util.List<? extends CharSequence>> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<Object>> a = new A<>();")
	@Test
	void bound_changed_to_generic_wildcard_super() {
		var v1 = "public class A<T extends java.util.List<? super CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<?>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_changed_from_generic_wildcard_super() {
		var v1 = "public class A<T extends java.util.List<?>> {}";
		var v2 = "public class A<T extends java.util.List<? super CharSequence>> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_generic_wildcard_to_type() {
		var v1 = "public class A<T extends java.util.List<? extends CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<CharSequence>> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<String>> a = new A<>();")
	@Test
	void bound_type_to_compatible_wildcard() {
		var v1 = "public class A<T extends java.util.List<String>> {}";
		var v2 = "public class A<T extends java.util.List<? extends String>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A<java.util.List<CharSequence>> a = new A<>();")
	@Test
	void bound_type_to_incompatible_wildcard() {
		var v1 = "public class A<T extends java.util.List<CharSequence>> {}";
		var v2 = "public class A<T extends java.util.List<? extends String>> {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Client("A<java.util.List<CharSequence>, String> a = new A();")
	@Test
	void unchanged_type_params_bounds() {
		var v1 = "public class A<T extends java.util.List<? super U>, U> {}";
		var v2 = "public class A<T extends java.util.List<? super U>, U> {}";

		assertNoBC(buildDiff(v1, v2));
	}
}
