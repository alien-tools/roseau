package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertBC;
import static com.github.maracas.roseau.TestUtils.assertNoBC;
import static com.github.maracas.roseau.TestUtils.buildDiff;

class TypeFormalTypeParameterChangedTest {
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
}
