package io.github.alien.roseau.diff;

import io.github.alien.roseau.RoseauOptions;
import io.github.alien.roseau.api.model.API;
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
		var v1 = TestUtils.buildSpoonAPI(v1src, exclude);
		var v2 = TestUtils.buildSpoonAPI(v2src, exclude);
		var report = new APIDiff(v1, v2).diff();

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

		API v1 = TestUtils.buildSpoonAPI(v1src, exclude);
		API v2 = TestUtils.buildSpoonAPI(v2src, new RoseauOptions.Exclude(List.of(), List.of()));

		var report = new APIDiff(v1, v2).diff();

		assertThat(report.getAllBreakingChanges()).hasSize(2);
		assertThat(report.getBreakingChanges()).hasSize(1);
	}
}
