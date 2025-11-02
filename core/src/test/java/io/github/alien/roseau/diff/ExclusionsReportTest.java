package io.github.alien.roseau.diff;

import io.github.alien.roseau.RoseauOptions;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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

		var exclude = new RoseauOptions.Exclude(List.of("p\\.api\\.C*"), List.of());
		var v1 = TestUtils.buildSourcesAPI(v1src, exclude);
		var v2 = TestUtils.buildSourcesAPI(v2src, exclude);
		var report = new ApiDiff().compare(v1, v2);

		assertThat(report.getAllBreakingChanges()).hasSize(1);
		assertThat(report.getBreakingChanges()).isEmpty();
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

		var exclude = new RoseauOptions.Exclude(List.of(),
			List.of(new RoseauOptions.AnnotationExclusion("p.api.Internal", Map.of("value", "alpha"))));
		var v1 = TestUtils.buildSourcesAPI(v1src, exclude);
		var v2 = TestUtils.buildSourcesAPI(v2src, exclude);
		var report = new ApiDiff().compare(v1, v2);

		assertThat(report.getAllBreakingChanges()).hasSize(2);
		assertThat(report.getBreakingChanges()).hasSize(1);
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

		var exclude = new RoseauOptions.Exclude(List.of(),
			List.of(new RoseauOptions.AnnotationExclusion("java.lang.Deprecated", Map.of())));
		var v1 = TestUtils.buildSourcesAPI(v1src, exclude);
		var v2 = TestUtils.buildSourcesAPI(v2src, exclude);
		var report = new ApiDiff().compare(v1, v2);

		assertThat(report.getAllBreakingChanges()).hasSize(1);
		assertThat(report.getBreakingChanges()).isEmpty();
	}
}
