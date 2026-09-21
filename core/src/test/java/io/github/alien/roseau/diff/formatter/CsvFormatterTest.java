package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.options.IgnoredCsvFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.alien.roseau.diff.formatter.ReportFixtures.normalize;
import static org.assertj.core.api.Assertions.assertThat;

class CsvFormatterTest {
	private static final int COLUMNS = 8;

	private static String csv(RoseauReport report) {
		return normalize(new CsvFormatter().format(report));
	}

	@Test
	void report_is_rendered_in_full() {
		assertThat(csv(ReportFixtures.mixed())).isEqualTo("""
			type;symbol;kind;nature;location;newSymbol;binaryBreaking;sourceBreaking
			pkg.A;pkg.A;FORMAL_TYPE_PARAMETER_REMOVED;MUTATION;pkg/A.java:3;;false;true
			pkg.A;pkg.A.generic(java.util.List<java.lang.String>);EXECUTABLE_PARAMETER_GENERICS_CHANGED;MUTATION;\
			pkg/A.java:6;pkg.A.generic(java.util.List<java.lang.Integer>);false;true
			pkg.A;pkg.A.generic(java.util.List<java.lang.String>);METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE;MUTATION;\
			pkg/A.java:6;pkg.A.generic(java.util.List<java.lang.Integer>);false;true
			pkg.A;pkg.A.mutated();METHOD_NOW_STATIC;MUTATION;pkg/A.java:5;pkg.A.mutated();true;false
			pkg.A;pkg.A.mutated();METHOD_OVERRIDABLE_NOW_STATIC;MUTATION;pkg/A.java:5;pkg.A.mutated();false;true
			pkg.A;pkg.A.removed();EXECUTABLE_REMOVED;DELETION;pkg/A.java:4;;true;true""");
	}

	@Test
	void empty_report_has_the_header() {
		assertThat(csv(ReportFixtures.empty())).isEqualTo(CsvFormatter.HEADER + "\n");
	}

	@Test
	void report_can_be_fed_back_as_an_ignore_list(@TempDir Path tempDir) throws IOException {
		var report = ReportFixtures.mixed();
		var ignored = tempDir.resolve("ignored.csv");
		Files.writeString(ignored, new CsvFormatter().format(report));
		var ignoredFile = new IgnoredCsvFile(ignored);
		assertThat(report.getBreakingChanges()).isNotEmpty().allMatch(ignoredFile::isIgnored);
	}

	@Test
	void unknown_locations_leave_the_column_empty() {
		assertThat(csv(ReportFixtures.reportedAt(SourceLocation.NO_LOCATION)).lines().skip(1))
			.containsExactly("pkg.A;pkg.A.m();EXECUTABLE_REMOVED;DELETION;;;true;true");
	}

	@Test
	void locations_without_a_line_report_the_file() {
		assertThat(csv(ReportFixtures.reportedAt(new SourceLocation(Path.of("pkg/A.class"), -1))).lines().skip(1))
			.containsExactly("pkg.A;pkg.A.m();EXECUTABLE_REMOVED;DELETION;pkg/A.class;;true;true");
	}
}
