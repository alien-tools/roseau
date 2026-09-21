package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.SourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static io.github.alien.roseau.diff.formatter.ReportFixtures.normalize;
import static org.assertj.core.api.Assertions.assertThat;

class CliFormatterTest {
	private static final String ANSI_ESCAPE = "\\[[0-9;]*m";

	private static String plain() {
		return normalize(new CliFormatter(CliFormatter.Mode.PLAIN).format(ReportFixtures.mixed()));
	}

	private static String ansi() {
		return normalize(new CliFormatter(CliFormatter.Mode.ANSI).format(ReportFixtures.mixed()));
	}

	@Test
	void plain_report_is_rendered_in_full() {
		assertThat(plain()).isEqualTo("""
			Breaking Changes found: 6 (2 binary-breaking, 5 source-breaking)
			⚠ pkg.A FORMAL_TYPE_PARAMETER_REMOVED [T]
			  ✓ binary-compatible ✗ source-breaking
			  → pkg/A.java:3
			⚠ pkg.A.generic(java.util.List<java.lang.String>) EXECUTABLE_PARAMETER_GENERICS_CHANGED \
			[java.util.List<java.lang.String> → java.util.List<java.lang.Integer>]
			  ✓ binary-compatible ✗ source-breaking
			  → pkg/A.java:6
			⚠ pkg.A.generic(java.util.List<java.lang.String>) METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE \
			[java.util.List<java.lang.String> → java.util.List<java.lang.Integer>]
			  ✓ binary-compatible ✗ source-breaking
			  → pkg/A.java:6
			⚠ pkg.A.mutated() METHOD_NOW_STATIC
			  ✗ binary-breaking ✓ source-compatible
			  → pkg/A.java:5
			⚠ pkg.A.mutated() METHOD_OVERRIDABLE_NOW_STATIC
			  ✓ binary-compatible ✗ source-breaking
			  → pkg/A.java:5
			✗ pkg.A.removed() EXECUTABLE_REMOVED
			  ✗ binary-breaking ✗ source-breaking
			  → pkg/A.java:4
			""");
	}

	@Test
	void ansi_only_adds_escapes_around_the_plain_report() {
		assertThat(ansi().replaceAll(ANSI_ESCAPE, "")).isEqualTo(plain());
	}

	@Test
	void inherited_changes_name_the_type_they_impact() {
		assertThat(normalize(new CliFormatter(CliFormatter.Mode.PLAIN).format(ReportFixtures.inherited())))
			.isEqualTo("""
				Breaking Changes found: 2 (2 binary-breaking, 2 source-breaking)
				✗ pkg.Base.removed() EXECUTABLE_REMOVED
				  ✗ binary-breaking ✗ source-breaking
				  → pkg/Base.java:4
				✗ pkg.Base.removed() in pkg.Child EXECUTABLE_REMOVED
				  ✗ binary-breaking ✗ source-breaking
				  → pkg/Base.java:4
				""");
	}

	@Test
	void empty_report_says_so() {
		assertThat(new CliFormatter(CliFormatter.Mode.PLAIN).format(ReportFixtures.empty()))
			.isEqualTo("No breaking changes found.");
	}

	@Test
	void unknown_locations_are_spelled_out() {
		var report = normalize(new CliFormatter(CliFormatter.Mode.PLAIN)
			.format(ReportFixtures.reportedAt(SourceLocation.NO_LOCATION)));

		assertThat(report).contains("  No source location");
	}

	@Test
	void locations_without_a_line_still_report_their_file() {
		var report = normalize(new CliFormatter(CliFormatter.Mode.PLAIN)
			.format(ReportFixtures.reportedAt(new SourceLocation(Path.of("pkg/A.class"), -1))));

		assertThat(report).contains("  → pkg/A.class:-1");
	}
}
