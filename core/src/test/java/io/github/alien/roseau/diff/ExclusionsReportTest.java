package io.github.alien.roseau.diff;

import io.github.alien.roseau.DiffPolicy;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ExclusionsReportTest {
	@Test
	void filter_breaking_changes_by_name() {
		var v1src = """
			module m { exports p.api; }
			package p.api;
			public class C { public void m() {} }""";
		var v2src = """
			module m { exports p.api; }
			package p.api;
			public class C {}""";

		var names = List.of("p\\.api\\.C*");
		var v1 = TestUtils.buildSourcesAPI(v1src);
		var v2 = TestUtils.buildSourcesAPI(v2src);
		var report = Roseau.diff(v1, v2);
		var filteredReport = report.filter(toPolicy(names, List.of()));

		assertThat(report.breakingChanges()).hasSize(1);
		assertThat(filteredReport.breakingChanges()).isEmpty();
	}

	@Test
	void filter_breaking_changes_by_annotation() {
		var v1src = """
			module m { exports p.api; }
			package p.api;
			public @interface Internal { String value(); }
			public class A { @Internal("alpha") public void m() {} }
			public class B { @Internal("beta") public void m() {} }""";
		var v2src = """
			module m { exports p.api; }
			package p.api;
			public @interface Internal { String value(); }
			public class A { }
			public class B { }""";

		var exclude = List.of(new DiffPolicy.AnnotationExclusion(
			"p.api.Internal", Map.of("value", "alpha")));
		var v1 = TestUtils.buildSourcesAPI(v1src);
		var v2 = TestUtils.buildSourcesAPI(v2src);
		var report = Roseau.diff(v1, v2);
		var filteredReport = report.filter(toPolicy(List.of(), exclude));

		assertThat(report.breakingChanges()).hasSize(2);
		assertThat(filteredReport.breakingChanges()).hasSize(1);
	}

	@Test
	void name_exclusions_propagate_to_nested_symbols() {
		var v1src = """
			module m { exports p.api; }
			package p.api;
			public class Excluded {
				public static class Inner { public void m() {} }
				public void x() {}
			}""";
		var v2src = """
			module m { exports p.api; }
			package p.api;
			public class Excluded {
				public static class Inner {}
			}""";

		var exclude = List.of("p\\.api\\.Excluded");
		var v1 = TestUtils.buildSourcesAPI(v1src);
		var v2 = TestUtils.buildSourcesAPI(v2src);
		var report = Roseau.diff(v1, v2);
		var filteredReport = report.filter(toPolicy(exclude, List.of()));

		assertThat(report.breakingChanges()).hasSize(2);
		assertThat(filteredReport.breakingChanges()).isEmpty();
	}

	@Test
	void annotation_exclusions_propagate() {
		var v1src = """
			@Deprecated
			public class C1 {
				public class C2 {
					public class C3 {
						public void m() {}
					}
				}
			}""";
		var v2src = """
			@Deprecated
			public class C1 {
				public class C2 {
					public class C3 {
						public void m(int i) {}
					}
				}
			}""";

		var exclude = List.of(new DiffPolicy.AnnotationExclusion(
			"java.lang.Deprecated", Map.of()));
		var v1 = TestUtils.buildSourcesAPI(v1src);
		var v2 = TestUtils.buildSourcesAPI(v2src);
		var report = Roseau.diff(v1, v2);
		var filteredReport = report.filter(toPolicy(List.of(), exclude));

		assertThat(report.breakingChanges()).hasSize(1);
		assertThat(filteredReport.breakingChanges()).isEmpty();
	}

	private static DiffPolicy toPolicy(List<String> names, List<DiffPolicy.AnnotationExclusion> annotations) {
		return DiffPolicy.builder()
			.excludeNames(names.stream().map(Pattern::compile).toList())
			.excludeAnnotations(annotations)
			.build();
	}
}
