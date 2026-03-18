package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class DependencyClasspathChangesTest {
	static final List<Path> CP_V1 = List.of(Path.of("src/test/resources/dependency-classpath-v1.jar"));
	static final List<Path> CP_V2 = List.of(Path.of("src/test/resources/dependency-classpath-v2.jar"));

	@Test
	void inherited_abstract_method_added_in_dependency() {
		var v1 = "public abstract class A extends depfixtures.abstractadded.B {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, 1, buildDiff(v1, CP_V1, v1, CP_V2));
	}

	@Test
	void inherited_method_removed_in_dependency() {
		var v1 = "public class A extends depfixtures.removed.B {}";

		assertBC("A", "depfixtures.removed.B.m()", BreakingChangeKind.EXECUTABLE_REMOVED, 1, buildDiff(v1, CP_V1, v1, CP_V2));
	}

	@Test
	void inherited_method_now_final_in_dependency() {
		var v1 = "public class A extends depfixtures.finalized.B {}";

		assertBC("A", "depfixtures.finalized.B.m()", BreakingChangeKind.METHOD_NOW_FINAL, 1, buildDiff(v1, CP_V1, v1, CP_V2));
	}

	@Test
	void inherited_method_now_static_in_dependency() {
		var v1 = "public class A extends depfixtures.staticed.B {}";

		assertBCs(buildDiff(v1, CP_V1, v1, CP_V2),
			bc("A", "depfixtures.staticed.B.m()", BreakingChangeKind.METHOD_NOW_STATIC, 1),
			bc("A", "depfixtures.staticed.B.m()", BreakingChangeKind.METHOD_OVERRIDABLE_NOW_STATIC, 1));
	}

	@Test
	void inherited_method_now_throws_checked_exception_in_dependency() {
		var v1 = "public class A extends depfixtures.throwschecked.B {}";

		assertBC("A", "depfixtures.throwschecked.B.m()", BreakingChangeKind.EXECUTABLE_NOW_THROWS_CHECKED_EXCEPTION, 1,
			buildDiff(v1, CP_V1, v1, CP_V2));
	}

	@Test
	void inherited_field_type_changed_in_dependency() {
		var v1 = "public class A extends depfixtures.fieldtype.B {}";

		assertBCs(buildDiff(v1, CP_V1, v1, CP_V2),
			bc("A", "depfixtures.fieldtype.B.f", BreakingChangeKind.FIELD_TYPE_CHANGED_INCOMPATIBLE, 1),
			bc("A", "depfixtures.fieldtype.B.f", BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED, 1));
	}

	@Test
	void inherited_exported_supertype_removed_in_dependency() {
		var v1 = "public class A extends depfixtures.supertype.B {}";

		assertBC("A", "A", BreakingChangeKind.TYPE_SUPERTYPE_REMOVED, 1, buildDiff(v1, CP_V1, v1, CP_V2));
	}

	@Test
	void inherited_exception_becomes_checked_in_dependency() {
		var v1 = "public class A extends depfixtures.checkedexc.B {}";

		assertBCs(buildDiff(v1, CP_V1, v1, CP_V2),
			bc("A", "A", BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, 1),
			bc("A", "A", BreakingChangeKind.TYPE_SUPERTYPE_REMOVED, 1));
	}
}
