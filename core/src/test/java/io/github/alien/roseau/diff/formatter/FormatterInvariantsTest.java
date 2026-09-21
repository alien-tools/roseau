package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.diff.RoseauReport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class FormatterInvariantsTest {
	private static String format(BreakingChangesFormatterFactory factory, RoseauReport report) {
		return BreakingChangesFormatterFactory.newBreakingChangesFormatter(factory).format(report);
	}

	@ParameterizedTest
	@EnumSource(BreakingChangesFormatterFactory.class)
	void formatting_is_repeatable(BreakingChangesFormatterFactory factory) {
		RoseauReport report = ReportFixtures.mixed();

		assertThat(volatileStripped(format(factory, report))).isEqualTo(volatileStripped(format(factory, report)));
	}

	@ParameterizedTest
	@EnumSource(BreakingChangesFormatterFactory.class)
	void formatting_is_ordered(BreakingChangesFormatterFactory factory) {
		RoseauReport report = ReportFixtures.mixed();

		assertThat(volatileStripped(format(factory, ReportFixtures.reversed(report))))
			.isEqualTo(volatileStripped(format(factory, report)));
	}

	@ParameterizedTest
	@EnumSource(BreakingChangesFormatterFactory.class)
	void excluded_changes_never_reach_the_output(BreakingChangesFormatterFactory factory) {
		RoseauReport report = ReportFixtures.excluded();

		assertThat(report.getAllBreakingChanges()).hasSize(6);
		assertThat(report.getBreakingChanges()).hasSize(5);
		assertThat(format(factory, report)).doesNotContain("removed()");
	}

	@ParameterizedTest
	@EnumSource(BreakingChangesFormatterFactory.class)
	void empty_reports_are_not_blank(BreakingChangesFormatterFactory factory) {
		assertThat(format(factory, ReportFixtures.empty())).isNotBlank();
	}

	// Drops the HTML timestamp
	private static String volatileStripped(String output) {
		return output.replaceAll("Generated [^<]*", "Generated <timestamp>");
	}
}
