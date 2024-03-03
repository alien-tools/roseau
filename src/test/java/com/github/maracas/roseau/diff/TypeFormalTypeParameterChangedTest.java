package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertBC;
import static com.github.maracas.roseau.utils.TestUtils.assertNoBC;
import static com.github.maracas.roseau.utils.TestUtils.buildDiff;

class TypeFormalTypeParameterChangedTest {
	@Test
	void param_renamed() {
		String v1 = "public class A<T> {}";
		String v2 = "public class A<U> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void param_swapped() {
		String v1 = "public class A<T, U> {}";
		String v2 = "public class A<U, T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bounded_param_swapped() {
		String v1 = "public class A<T extends String, U extends CharSequence> {}";
		String v2 = "public class A<U extends CharSequence, T extends String> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_added() {
		String v1 = "public class A<T> {}";
		String v2 = "public class A<T extends String> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_object_added() {
		String v1 = "public class A<T> {}";
		String v2 = "public class A<T extends Object> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bound_removed() {
		String v1 = "public class A<T extends String> {}";
		String v2 = "public class A<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void second_bound_removed() {
		String v1 = "public class A<T extends String & Runnable> {}";
		String v2 = "public class A<T extends String> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bound_param_removed() {
		String v1 = "public class A<T extends U, U> {}";
		String v2 = "public class A<T, U> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bound_param_added() {
		String v1 = "public class A<T, U> {}";
		String v2 = "public class A<T, U extends T> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_modified_compatible() {
		String v1 = "public class A<T extends String> {}";
		String v2 = "public class A<T extends CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bound_modified_incompatible() {
		String v1 = "public class A<T extends CharSequence> {}";
		String v2 = "public class A<T extends String> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_modified_incompatible_param_1() {
		// Still breaking if client chooses a U that is not a subtype of T
		String v1 = "public class A<T extends String, U extends CharSequence, V extends T> {}";
		String v2 = "public class A<T extends String, U extends CharSequence, V extends U> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_modified_incompatible_param_2() {
		String v1 = "public class A<T extends String, U extends CharSequence, V extends U> {}";
		String v2 = "public class A<T extends String, U extends CharSequence, V extends T> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void second_bound_added_compatible() {
		String v1 = "public class A<T extends String> {}";
		String v2 = "public class A<T extends String & CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void second_bound_added_incompatible() {
		String v1 = "public class A<T extends String> {}";
		String v2 = "public class A<T extends String & Runnable> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_changed_to_compatible_generic() {
		String v1 = "public class A<T extends java.util.List<? extends String>> {}";
		String v2 = "public class A<T extends java.util.List<? extends CharSequence>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bound_changed_to_incompatible_generic() {
		String v1 = "public class A<T extends java.util.List<? extends CharSequence>> {}";
		String v2 = "public class A<T extends java.util.List<? extends String>> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_changed_to_generic_supertype() {
		String v1 = "public class A<T extends java.util.ArrayList<? extends String>> {}";
		String v2 = "public class A<T extends java.util.List<? extends CharSequence>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bound_changed_to_generic_subtype() {
		String v1 = "public class A<T extends java.util.List<? extends String>> {}";
		String v2 = "public class A<T extends java.util.ArrayList<? extends CharSequence>> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_changed_to_compatible_generic_super() {
		String v1 = "public class A<T extends java.util.List<? super CharSequence>> {}";
		String v2 = "public class A<T extends java.util.List<? super String>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bound_changed_to_incompatible_generic_super() {
		String v1 = "public class A<T extends java.util.List<? super String>> {}";
		String v2 = "public class A<T extends java.util.List<? super CharSequence>> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_changed_to_incompatible_type_super() {
		String v1 = "public class A<T extends java.util.ArrayList<? super CharSequence>> {}";
		String v2 = "public class A<T extends java.util.List<? super String>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bound_changed_to_incompatible_subtype_super() {
		String v1 = "public class A<T extends java.util.List<? super CharSequence>> {}";
		String v2 = "public class A<T extends java.util.ArrayList<? super String>> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_changed_to_generic_wildcard_extends() {
		String v1 = "public class A<T extends java.util.List<? extends CharSequence>> {}";
		String v2 = "public class A<T extends java.util.List<?>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bound_changed_from_generic_wildcard_extends() {
		String v1 = "public class A<T extends java.util.List<?>> {}";
		String v2 = "public class A<T extends java.util.List<? extends CharSequence>> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_changed_to_generic_wildcard_super() {
		String v1 = "public class A<T extends java.util.List<? super CharSequence>> {}";
		String v2 = "public class A<T extends java.util.List<?>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bound_changed_from_generic_wildcard_super() {
		String v1 = "public class A<T extends java.util.List<?>> {}";
		String v2 = "public class A<T extends java.util.List<? super CharSequence>> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_generic_wildcard_to_type() {
		String v1 = "public class A<T extends java.util.List<? extends String>> {}";
		String v2 = "public class A<T extends java.util.List<String>> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void bound_type_to_compatible_wildcard() {
		String v1 = "public class A<T extends java.util.List<String>> {}";
		String v2 = "public class A<T extends java.util.List<? extends String>> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void bound_type_to_incompatible_wildcard() {
		String v1 = "public class A<T extends java.util.List<CharSequence>> {}";
		String v2 = "public class A<T extends java.util.List<? extends String>> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void unchanged_type_params_bounds() {
		String v1 = "public class A<T extends java.util.List<? super U>, U>> {}";
		String v2 = "public class A<T extends java.util.List<? super U>, U>> {}";

		assertNoBC(buildDiff(v1, v2));
	}
}
