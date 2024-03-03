package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertBC;
import static com.github.maracas.roseau.TestUtils.assertNoBC;
import static com.github.maracas.roseau.TestUtils.buildDiff;

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
	void upper_bound_added() {
		String v1 = "public class A<T> {}";
		String v2 = "public class A<T extends String> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void upper_bound_object_added() {
		String v1 = "public class A<T> {}";
		String v2 = "public class A<T extends Object> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void upper_bound_removed() {
		String v1 = "public class A<T extends String> {}";
		String v2 = "public class A<T> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void second_upper_bound_removed() {
		String v1 = "public class A<T extends String & Runnable> {}";
		String v2 = "public class A<T extends String> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void upper_bound_param_removed() {
		String v1 = "public class A<T extends U, U> {}";
		String v2 = "public class A<T, U> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void upper_bound_param_added() {
		String v1 = "public class A<T, U> {}";
		String v2 = "public class A<T, U extends T> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void upper_bound_modified_compatible() {
		String v1 = "public class A<T extends String> {}";
		String v2 = "public class A<T extends CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void upper_bound_modified_incompatible() {
		String v1 = "public class A<T extends CharSequence> {}";
		String v2 = "public class A<T extends String> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void upper_bound_modified_compatible_param() {
		String v1 = "public class A<T extends String, U extends CharSequence, V extends T> {}";
		String v2 = "public class A<T extends String, U extends CharSequence, V extends U> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void upper_bound_modified_incompatible_param() {
		String v1 = "public class A<T extends String, U extends CharSequence, V extends U> {}";
		String v2 = "public class A<T extends String, U extends CharSequence, V extends T> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void second_upper_bound_added_compatible() {
		String v1 = "public class A<T extends String> {}";
		String v2 = "public class A<T extends String & CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void second_upper_bound_added_incompatible() {
		String v1 = "public class A<T extends String> {}";
		String v2 = "public class A<T extends String & Runnable> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void upper_bound_generic_extends_changed_bound_compatible() {
		String v1 = "public class A<T extends java.util.List<? extends String> {}";
		String v2 = "public class A<T extends java.util.List<? extends CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void upper_bound_generic_extends_changed_bound_incompatible() {
		String v1 = "public class A<T extends java.util.List<? extends CharSequence> {}";
		String v2 = "public class A<T extends java.util.List<? extends String> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void upper_bound_generic_extends_changed_type_compatible() {
		String v1 = "public class A<T extends java.util.ArrayList<? extends String> {}";
		String v2 = "public class A<T extends java.util.List<? extends CharSequence> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void upper_bound_generic_extends_changed_type_incompatible() {
		String v1 = "public class A<T extends java.util.List<? extends String> {}";
		String v2 = "public class A<T extends java.util.ArrayList<? extends CharSequence> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void upper_bound_generic_super_changed_bound_compatible() {
		String v1 = "public class A<T extends java.util.List<? super CharSequence> {}";
		String v2 = "public class A<T extends java.util.List<? super String> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void upper_bound_generic_super_changed_bound_incompatible() {
		String v1 = "public class A<T extends java.util.List<? super String> {}";
		String v2 = "public class A<T extends java.util.List<? super CharSequence> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void upper_bound_generic_super_changed_type_compatible() {
		String v1 = "public class A<T extends java.util.ArrayList<? super CharSequence> {}";
		String v2 = "public class A<T extends java.util.List<? super String> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void upper_bound_generic_super_changed_type_incompatible() {
		String v1 = "public class A<T extends java.util.List<? super CharSequence> {}";
		String v2 = "public class A<T extends java.util.ArrayListList<? super String> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void upper_bound_generic_to_wildcard() {
		String v1 = "public class A<T extends java.util.List<? extends CharSequence> {}";
		String v2 = "public class A<T extends java.util.List<?> {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void upper_bound_generic_from_wildcard() {
		String v1 = "public class A<T extends java.util.List<?> {}";
		String v2 = "public class A<T extends java.util.List<? extends CharSequence> {}";

		assertBC("A", BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, 1, buildDiff(v1, v2));
	}

	@Test
	void unchanged_type_params_bounds() {
		String v1 = "public class A<T extends java.util.List<? super U>, U> {}";
		String v2 = "public class A<T extends java.util.List<? super U>, U> {}";

		assertNoBC(buildDiff(v1, v2));
	}
}
