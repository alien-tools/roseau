package io.github.alien.roseau;

import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DiffPolicyTest {
	private static final String V1 = """
		package pkg;
		public class T<U> {
			public void m() {}
			public void n() {}
		}""";

	private static final String V2 = """
		package pkg;
		public class T {
			public static void n() {}
		}""";

	@Test
	void source_only_scope_keeps_only_source_breaking_changes() {
		var report = diffSimpleChange();
		var filtered = report.filter(DiffPolicy.builder().scope(DiffPolicy.Scope.SOURCE_ONLY).build());

		assertThat(report.breakingChanges().stream().anyMatch(bc -> !bc.kind().isSourceBreaking())).isTrue();
		assertThat(filtered.breakingChanges().stream().allMatch(bc -> bc.kind().isSourceBreaking())).isTrue();
	}

	@Test
	void binary_only_scope_keeps_only_binary_breaking_changes() {
		var report = diffSimpleChange();
		var filtered = report.filter(DiffPolicy.builder().scope(DiffPolicy.Scope.BINARY_ONLY).build());

		assertThat(report.breakingChanges().stream().anyMatch(bc -> !bc.kind().isBinaryBreaking())).isTrue();
		assertThat(filtered.breakingChanges().stream().allMatch(bc -> bc.kind().isBinaryBreaking())).isTrue();
	}

	@Test
	void ignored_breaking_changes_match_type_symbol_and_kind_exactly() {
		var report = diffSimpleChange();

		var filtered = report.filter(DiffPolicy.builder()
			.ignoreBreakingChanges(List.of(
				new DiffPolicy.IgnoredBreakingChange("pkg.T", "pkg.T.m()", BreakingChangeKind.EXECUTABLE_REMOVED),
				new DiffPolicy.IgnoredBreakingChange("pkg.T", "pkg.T.n()", BreakingChangeKind.METHOD_NOW_STATIC)
			))
			.build());

		assertThat(filtered.breakingChanges())
			.extracting(BreakingChange::kind)
			.doesNotContain(BreakingChangeKind.EXECUTABLE_REMOVED)
			.contains(
				BreakingChangeKind.FORMAL_TYPE_PARAMETER_REMOVED,
				BreakingChangeKind.METHOD_OVERRIDABLE_NOW_STATIC
			);
	}

	@Test
	void annotation_exclusion_arguments_must_match_exactly() {
		var v1 = TestUtils.buildSourcesAPI("""
			package p;
			public @interface Internal { String value(); }
			public class A { @Internal("alpha") public void m() {} }
			public class B { @Internal("beta") public void m() {} }""");
		var v2 = TestUtils.buildSourcesAPI("""
			package p;
			public @interface Internal { String value(); }
			public class A { }
			public class B { }""");

		var report = Roseau.diff(v1, v2);
		var filtered = report.filter(DiffPolicy.builder()
			.excludeAnnotations(List.of(new DiffPolicy.AnnotationExclusion("p.Internal", Map.of("value", "alpha"))))
			.build());

		assertThat(filtered.breakingChanges()).hasSize(1);
	}

	@Test
	void name_exclusions_can_filter_an_entire_type() {
		var report = diffSimpleChange();
		var filtered = report.filter(DiffPolicy.builder()
			.excludeNames(List.of(Pattern.compile("pkg\\.T")))
			.build());

		assertThat(filtered.breakingChanges()).isEmpty();
	}

	@Test
	void empty_policy_retains_all_breaking_changes() {
		var report = diffSimpleChange();
		var filtered = report.filter(DiffPolicy.empty());

		assertThat(filtered).isEqualTo(report);
	}

	@Test
	void scope_and_ignores_compose_independently() {
		var report = diffSimpleChange();

		// BINARY_ONLY keeps EXECUTABLE_REMOVED and METHOD_NOW_STATIC;
		// then ignore EXECUTABLE_REMOVED -> only METHOD_NOW_STATIC remains
		var filtered = report.filter(DiffPolicy.builder()
			.scope(DiffPolicy.Scope.BINARY_ONLY)
			.ignoreBreakingChanges(List.of(
				new DiffPolicy.IgnoredBreakingChange("pkg.T", "pkg.T.m()", BreakingChangeKind.EXECUTABLE_REMOVED)
			))
			.build());

		assertThat(filtered.breakingChanges()).hasSize(1);
	}

	@Test
	void scope_and_name_exclusion_compose() {
		var v1 = TestUtils.buildSourcesAPI("""
			package p;
			public class Keep { public void m() {} }
			public class Drop { public void m() {} }""");
		var v2 = TestUtils.buildSourcesAPI("""
			package p;
			public class Keep { }
			public class Drop { }""");

		var report = Roseau.diff(v1, v2);
		var filtered = report.filter(DiffPolicy.builder()
			.scope(DiffPolicy.Scope.SOURCE_ONLY)
			.excludeNames(List.of(Pattern.compile("p\\.Drop")))
			.build());

		assertThat(filtered.breakingChanges()).hasSize(1);
	}

	@Test
	void annotation_exclusion_without_args_matches_any_annotation_arguments() {
		var v1 = TestUtils.buildSourcesAPI("""
			package p;
			public @interface Tag { String value(); }
			public class A { @Tag("x") public void m() {} }
			public class B { @Tag("y") public void m() {} }""");
		var v2 = TestUtils.buildSourcesAPI("""
			package p;
			public @interface Tag { String value(); }
			public class A { }
			public class B { }""");

		var report = Roseau.diff(v1, v2);
		var filtered = report.filter(DiffPolicy.builder()
			.excludeAnnotations(List.of(new DiffPolicy.AnnotationExclusion("p.Tag", Map.of())))
			.build());

		assertThat(filtered.breakingChanges()).isEmpty();
	}

	@Test
	void ignored_bc_with_wrong_kind_does_not_match() {
		var report = diffSimpleChange();
		var filtered = report.filter(DiffPolicy.builder()
			.ignoreBreakingChanges(List.of(
				new DiffPolicy.IgnoredBreakingChange("pkg.T", "pkg.T.m()", BreakingChangeKind.EXECUTABLE_NOW_PROTECTED)
			))
			.build());

		assertThat(filtered.breakingChanges()).isEqualTo(report.breakingChanges());
	}

	private static RoseauReport diffSimpleChange() {
		return Roseau.diff(TestUtils.buildSourcesAPI(V1), TestUtils.buildSourcesAPI(V2));
	}
}
