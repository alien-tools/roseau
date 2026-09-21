package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.RoseauReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static io.github.alien.roseau.diff.formatter.ReportFixtures.normalize;
import static org.assertj.core.api.Assertions.assertThat;

class MdFormatterTest {
	private static final int COLUMNS = 8;

	private static String md(RoseauReport report) {
		return normalize(new MdFormatter().format(report));
	}

	@Test
	void report_is_rendered_in_full() {
		assertThat(md(ReportFixtures.mixed())).isEqualTo("""
			## Breaking Changes Report
			6 breaking changes detected (2 binary-breaking, 5 source-breaking).

			| Type | Symbol | Kind | Nature | Location | New symbol | Binary | Source |
			|------|--------|------|--------|----------|------------|--------|--------|
			| pkg.A | pkg.A | FORMAL_TYPE_PARAMETER_REMOVED | MUTATION | pkg/A.java:3 |  | false | true |
			| pkg.A | pkg.A.generic(java.util.List&lt;java.lang.String&gt;) | EXECUTABLE_PARAMETER_GENERICS_CHANGED \
			| MUTATION | pkg/A.java:6 | pkg.A.generic(java.util.List&lt;java.lang.Integer&gt;) | false | true |
			| pkg.A | pkg.A.generic(java.util.List&lt;java.lang.String&gt;) | METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE \
			| MUTATION | pkg/A.java:6 | pkg.A.generic(java.util.List&lt;java.lang.Integer&gt;) | false | true |
			| pkg.A | pkg.A.mutated() | METHOD_NOW_STATIC | MUTATION | pkg/A.java:5 | pkg.A.mutated() | true | false |
			| pkg.A | pkg.A.mutated() | METHOD_OVERRIDABLE_NOW_STATIC | MUTATION | pkg/A.java:5 | pkg.A.mutated() \
			| false | true |
			| pkg.A | pkg.A.removed() | EXECUTABLE_REMOVED | DELETION | pkg/A.java:4 |  | true | true |
			""");
	}

	@Test
	void generics_in_symbols_are_escaped() {
		String report = md(ReportFixtures.mixed());

		assertThat(report)
			.contains("pkg.A.generic(java.util.List&lt;java.lang.String&gt;)")
			.doesNotContain("<java.lang.String>");
	}

	@Test
	void every_row_has_the_same_number_of_cells_as_the_header() {
		List<String> rows = md(ReportFixtures.mixed()).lines()
			.filter(line -> line.startsWith("|"))
			.toList();

		assertThat(rows).hasSize(2 + ReportFixtures.mixed().getBreakingChanges().size());
		assertThat(rows).allSatisfy(row -> assertThat(cells(row)).hasSize(COLUMNS));
	}

	@Test
	void empty_report_skips_the_table() {
		assertThat(md(ReportFixtures.empty())).isEqualTo("""
			## Breaking Changes Report
			No breaking changes detected.""");
	}

	@Test
	void unknown_locations_are_spelled_out() {
		assertThat(md(ReportFixtures.reportedAt(SourceLocation.NO_LOCATION)))
			.contains("| pkg.A | pkg.A.m() | EXECUTABLE_REMOVED | DELETION | No location |  | true | true |");
	}

	@Test
	void locations_without_a_line_report_the_file_alone() {
		assertThat(md(ReportFixtures.reportedAt(new SourceLocation(Path.of("pkg/A.class"), -1))))
			.contains("| pkg.A | pkg.A.m() | EXECUTABLE_REMOVED | DELETION | pkg/A.class |  | true | true |");
	}

	private static List<String> cells(String row) {
		return List.of(row.substring(1, row.length() - 1).split("\\|", -1));
	}
}
