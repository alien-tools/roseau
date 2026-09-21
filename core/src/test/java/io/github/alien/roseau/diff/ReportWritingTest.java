package io.github.alien.roseau.diff;

import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RoseauReport#writeReport} is what {@code --report=FORMAT=PATH} and the Maven plug-in go through.
 */
class ReportWritingTest {
	@TempDir
	static Path sharedDir;

	private static RoseauReport report;

	@BeforeAll
	static void buildReport() {
		var v1 = """
			package pkg;

			public class A {
				public void removed() {}
			}""";
		var v2 = """
			package pkg;

			public class A {
			}""";
		report = Roseau.diff(TestUtils.buildSourcesAPI(v1), TestUtils.buildSourcesAPI(v2));
	}

	@ParameterizedTest
	@EnumSource(BreakingChangesFormatterFactory.class)
	void written_file_holds_the_formatted_report(BreakingChangesFormatterFactory format,
	                                             @TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("report");

		report.writeReport(format, file);

		assertThat(Files.readString(file, StandardCharsets.UTF_8))
			.isNotBlank()
			.contains("EXECUTABLE_REMOVED");
	}

	@Test
	void every_format_writes_its_own_syntax() {
		// Guards the wiring from format to formatter; the size check fails when a new format needs a line here
		assertThat(BreakingChangesFormatterFactory.values()).hasSize(5);
		assertThat(written(BreakingChangesFormatterFactory.CSV)).startsWith("type;symbol;kind");
		assertThat(written(BreakingChangesFormatterFactory.JSON)).startsWith("[");
		assertThat(written(BreakingChangesFormatterFactory.MD)).startsWith("## Breaking Changes Report");
		assertThat(written(BreakingChangesFormatterFactory.HTML)).startsWith("<!DOCTYPE html>");
		assertThat(written(BreakingChangesFormatterFactory.CLI)).startsWith("Breaking Changes found:");
	}

	@Test
	void cli_reports_are_written_without_terminal_escapes() {
		// The CLI formatter colors its output when it detects a terminal; a file is never one
		assertThat(written(BreakingChangesFormatterFactory.CLI)).doesNotContain("[");
	}

	@Test
	void missing_parent_directories_are_created(@TempDir Path tempDir) {
		Path file = tempDir.resolve("reports").resolve("nested").resolve("report.csv");

		report.writeReport(BreakingChangesFormatterFactory.CSV, file);

		assertThat(file).isNotEmptyFile();
	}

	@Test
	void unwritable_destinations_fail_with_a_user_facing_message(@TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("report.csv");
		Files.writeString(file, "");
		assertThat(file.toFile().setReadOnly()).isTrue();

		assertThatThrownBy(() -> report.writeReport(BreakingChangesFormatterFactory.CSV, file))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Error writing report to " + file);
	}

	private static String written(BreakingChangesFormatterFactory format) {
		try {
			Path file = sharedDir.resolve("report." + format.name());
			report.writeReport(format, file);
			return Files.readString(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
