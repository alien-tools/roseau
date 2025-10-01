package io.github.alien.roseau.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RoseauCLITest {
	CommandLine cmd;
	Writer out;
	Writer err;

	@BeforeEach
	void setUp() {
		out = new StringWriter();
		err = new StringWriter();
		cmd = new CommandLine(new RoseauCLI());
		cmd.setOut(new PrintWriter(out));
		cmd.setErr(new PrintWriter(err));
	}

	@Test
	void no_mode() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src");

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Missing required argument (specify one of these): (--api | --diff)");
	}

	// --- Diffs --- //
	@Test
	void simple_source_diff() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("METHOD_REMOVED pkg.T.m");
	}

	@Test
	void simple_jar_diff() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("METHOD_REMOVED pkg.T.m");
	}

	@Test
	void heterogeneous_diff_1() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("METHOD_REMOVED pkg.T.m");
	}

	@Test
	void heterogeneous_diff_2() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("METHOD_REMOVED pkg.T.m");
	}

	@Test
	void no_breaking_changes() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v1/test-project-v1.jar",
			"--diff");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("No breaking changes found.");
	}

	@Test
	void invalid_v1_path() {
		var exitCode = cmd.execute("--v1=src/test/resources/invalid-path",
			"--v2=src/test/resources/test-project-v2/test-project-v1.jar",
			"--diff");

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Cannot find v1:");
	}

	@Test
	void missing_v1() {
		var exitCode = cmd.execute("--api");

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Cannot find v1:");
	}

	@Test
	void missing_v2_in_diff() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--diff");

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Cannot find v2:");
	}

	@Test
	void fail_mode_with_breaking_changes() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--fail-on-bc");

		assertThat(exitCode).isOne();
	}

	@Test
	void no_fail_mode_with_breaking_changes() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(exitCode).isZero();
	}

	// --- APIs --- //
	@Test
	void write_api_no_file() {
		var defaultFile = Path.of("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api");

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("--api-json required in --api mode");
	}

	@Test
	void write_api_custom_file(@TempDir Path tempDir) {
		var jsonFile = tempDir.resolve("custom.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--api",
			"--api-json=" + jsonFile);

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("API has been written to " + jsonFile);
		assertThat(jsonFile).isNotEmptyFile();
	}

	@Test
	void write_api_spoon(@TempDir Path tempDir) {
		var jsonFile = tempDir.resolve("spoon.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--extractor=SPOON",
			"--api",
			"--api-json=" + jsonFile,
			"--verbose");

		assertThat(exitCode).isZero();
		assertThat(jsonFile).isNotEmptyFile();
		assertThat(out.toString())
			.contains("Extracting API from", "using SPOON")
			.contains("API has been written to " + jsonFile);
	}

	@Test
	void write_api_io_error(@TempDir Path tempDir) throws IOException {
		var apiFile = tempDir.resolve("api.json");
		Files.writeString(apiFile, "{}");
		apiFile.toFile().setReadOnly();

		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--api-json=" + apiFile);

		// Should succeed despite I/O error - CLI continues execution
		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Error writing API to " + apiFile);
	}

	// --- Options --- //
	@Test
	void missing_classpath() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Warning: no classpath provided, results may be inaccurate");
	}

	@Test
	void custom_classpath_deduplicated() {
		var cp = String.join(File.pathSeparator,
			"src/test/resources/test-project-v1/test-project-v1.jar",
			"src/test/resources/test-project-v2/test-project-v2.jar",
			"src/test/resources/test-project-v1/test-project-v1.jar"
		);
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--classpath=" + cp,
			"--verbose");

		assertThat(exitCode).isZero();
		assertThat(out.toString())
			.containsOnlyOnce("Classpath: [")
			.containsOnlyOnce("test-project-v1.jar")
			.containsOnlyOnce("test-project-v2.jar");
	}

	@Test
	void valid_pom(@TempDir Path tempDir) {
		var api = tempDir.resolve("api.json");
		var pom = Path.of("pom.xml");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v1/src",
			"--api",
			"--pom=" + pom,
			"--api-json=" + api,
			"--verbose");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Extracting classpath from " + pom);
	}

	@Test
	void corrupt_pom(@TempDir Path tempDir) {
		var api = tempDir.resolve("api.json");
		var pom = Path.of("src/test/resources/corrupt-pom.xml");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v1/src",
			"--api",
			"--pom=" + pom,
			"--api-json=" + api);

		assertThat(exitCode).isZero();
	}

	@Test
	void missing_pom(@TempDir Path tempDir) {
		var api = tempDir.resolve("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--pom=src/test/resources/none.xml",
			"--api-json=" + api);

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Cannot find pom:");
	}

	@Test
	void unsupported_extractor() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--extractor=UNKNOWN",
			"--api");

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Invalid value for option '--extractor'");
	}

	@Test
	void incompatible_extractor() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--extractor=ASM",
			"--diff");

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("ASM extractor cannot be used");
	}

	@Test
	void unsupported_formatter() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--formatter=UNKNOWN",
			"--diff");

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Unknown option: '--formatter=UNKNOWN'");
	}

	// --- Reports --- //
	@Test
	void diff_without_report() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(exitCode).isZero();
		assertThat(Path.of("report.csv")).doesNotExist();
	}

	@Test
	void write_report_custom(@TempDir Path tempDir) {
		var reportFile = tempDir.resolve("custom.csv");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--report=" + reportFile);

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Report has been written to " + reportFile);
		assertThat(reportFile).isNotEmptyFile();
	}

	@Test
	void write_report_html(@TempDir Path tempDir) {
		var reportFile = tempDir.resolve("report.html");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--format=HTML",
			"--report=" + reportFile);

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Report has been written to " + reportFile);
		assertThat(reportFile).isNotEmptyFile();
	}

	@Test
	void write_report_json(@TempDir Path tempDir) {
		var reportFile = tempDir.resolve("report.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--format=JSON",
			"--report=" + reportFile);

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Report has been written to " + reportFile);
		assertThat(reportFile).isNotEmptyFile();
	}

	@Test
	void write_report_markdown(@TempDir Path tempDir) {
		var reportFile = tempDir.resolve("report.md");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--format=MD",
			"--report=" + reportFile);

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Report has been written to " + reportFile);
		assertThat(reportFile).isNotEmptyFile();
	}

	@Test
	void write_report_io_error(@TempDir Path tempDir) throws IOException {
		var reportFile = tempDir.resolve("report.csv");
		Files.writeString(reportFile, "");
		reportFile.toFile().setReadOnly();

		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--report=" + reportFile);

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Error writing report to " + reportFile);
	}

	// --- Ignored --- //
	@Test
	void ignore_simple_bc(@TempDir Path tempDir) throws IOException {
		var ignored = tempDir.resolve("ignored.csv");
		Files.writeString(ignored, """
			type;symbol;kind;nature
			pkg.T;pkg.T.m();METHOD_REMOVED;DELETION""");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--ignored=" + ignored,
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).doesNotContain("METHOD_REMOVED pkg.T.m");
	}

	@Test
	void ignore_all_bcs_in_report(@TempDir Path tempDir) {
		var ignored = tempDir.resolve("ignored.csv");
		cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--report=" + ignored);

		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--ignored=" + ignored,
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("No breaking changes found.");
	}

	@Test
	void invalid_ignored_csv(@TempDir Path tempDir) {
		var invalidCsv = tempDir.resolve("nonexistent.csv");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--ignored=" + invalidCsv);

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Cannot find ignored CSV: " + invalidCsv);
	}

	@Test
	void malformed_ignored_csv(@TempDir Path tempDir) throws IOException {
		var malformedCsv = tempDir.resolve("malformed.csv");
		Files.writeString(malformedCsv, """
			type;symbol;kind;nature
			one_field;a_second_field""");

		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--ignored=" + malformedCsv,
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(err.toString()).contains("Malformed line");
	}

	@Test
	void verbose_mode_with_exception() {
		var exitCode = cmd.execute("--v1=src/test/resources/invalid-path",
			"--api",
			"--verbose");

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString())
			.contains("Cannot find v1:")
			.contains("io.github.alien.roseau.RoseauException");
	}

	@Test
	void plain_mode_formatting() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).doesNotContain("\u001B[");
	}

	@Test
	void colored_output_formatting() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("\u001B[");
	}

	@Test
	void corrupt_jar() {
		var exitCode = cmd.execute("--v1=src/test/resources/corrupt.jar",
			"--v2=src/test/resources/corrupt.jar",
			"--diff");

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Invalid path to library");
	}
}
