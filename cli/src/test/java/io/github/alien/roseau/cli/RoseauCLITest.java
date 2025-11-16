package io.github.alien.roseau.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

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

		assertThat(err.toString()).contains("Missing required argument (specify one of these): (--api | --diff)");
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	// --- Diffs --- //
	@Test
	void simple_source_diff() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--plain");

		assertThat(out.toString()).contains("pkg.T.m() METHOD_REMOVED");
		assertThat(out.toString()).contains("pkg.T TYPE_FORMAL_TYPE_PARAMETERS_REMOVED");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void simple_jar_diff() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--plain");

		assertThat(out.toString()).contains("pkg.T.m() METHOD_REMOVED");
		assertThat(out.toString()).contains("pkg.T TYPE_FORMAL_TYPE_PARAMETERS_REMOVED");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void heterogeneous_diff_1() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--plain");

		assertThat(out.toString()).contains("pkg.T.m() METHOD_REMOVED");
		assertThat(out.toString()).contains("pkg.T TYPE_FORMAL_TYPE_PARAMETERS_REMOVED");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void heterogeneous_diff_2() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--plain");

		assertThat(out.toString()).contains("pkg.T.m() METHOD_REMOVED");
		assertThat(out.toString()).contains("pkg.T TYPE_FORMAL_TYPE_PARAMETERS_REMOVED");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void no_breaking_changes() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v1/test-project-v1.jar",
			"--diff");

		assertThat(out.toString()).contains("No breaking changes found.");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void binary_only() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--binary-only",
			"--plain");

		assertThat(out.toString())
			.contains("METHOD_REMOVED")
			.doesNotContain("TYPE_FORMAL_TYPE_PARAMETERS_REMOVED")
			.contains("METHOD_NOW_STATIC");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void source_only() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--source-only",
			"--plain");

		assertThat(out.toString())
			.contains("METHOD_REMOVED")
			.contains("TYPE_FORMAL_TYPE_PARAMETERS_REMOVED")
			.doesNotContain("METHOD_NOW_STATIC");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void invalid_v1_path() {
		var exitCode = cmd.execute("--v1=src/test/resources/invalid-path",
			"--v2=src/test/resources/test-project-v2/test-project-v1.jar",
			"--diff");

		assertThat(err.toString()).contains("Cannot find v1:");
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	@Test
	void missing_v1() {
		var exitCode = cmd.execute("--api");

		assertThat(err.toString()).contains("Cannot find v1:");
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	@Test
	void missing_v2_in_diff() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--diff");

		assertThat(err.toString()).contains("Cannot find v2:");
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	@Test
	void fail_mode_with_breaking_changes() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--fail-on-bc");

		assertThat(exitCode).isEqualTo(ExitCode.BREAKING.code());
	}

	@Test
	void no_fail_mode_with_breaking_changes() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	// --- APIs --- //
	@Test
	void write_api_no_file() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api");

		assertThat(err.toString()).contains("Path to a JSON file required in --api mode");
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	@Test
	void write_api_custom_file(@TempDir Path tempDir) {
		var jsonFile = tempDir.resolve("custom.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--api",
			"--api-json=" + jsonFile);

		assertThat(jsonFile).isNotEmptyFile();
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
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
		assertThat(err.toString()).contains("Error writing API to " + apiFile);
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	// --- Options --- //
	@Test
	void missing_classpath() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(err.toString()).contains("Warning: no classpath provided", "results may be inaccurate");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void valid_pom(@TempDir Path tempDir) {
		var api = tempDir.resolve("api.json");
		var pom = Path.of("src/test/resources/valid-pom.xml");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--pom=" + pom,
			"--api-json=" + api);

		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
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

		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void missing_pom(@TempDir Path tempDir) {
		var api = tempDir.resolve("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--pom=src/test/resources/none.xml",
			"--api-json=" + api);

		assertThat(err.toString()).contains("Cannot find pom:");
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	@Test
	void unsupported_formatter() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--formatter=UNKNOWN",
			"--diff");

		assertThat(err.toString()).contains("Unknown option: '--formatter=UNKNOWN'");
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	// --- Reports --- //
	@Test
	void diff_without_report() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void write_report_without_format(@TempDir Path tempDir) {
		var reportFile = tempDir.resolve("custom.csv");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--report=" + reportFile);

		assertThat(err.toString()).contains("--format required with --report");
		assertThat(reportFile).doesNotExist();
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	@Test
	void write_report_custom(@TempDir Path tempDir) {
		var reportFile = tempDir.resolve("custom.csv");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--report=" + reportFile,
			"--format=CSV");

		assertThat(reportFile).isNotEmptyFile();
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void write_report_html(@TempDir Path tempDir) {
		var reportFile = tempDir.resolve("report.html");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--format=HTML",
			"--report=" + reportFile);

		assertThat(reportFile).isNotEmptyFile();
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void write_report_json(@TempDir Path tempDir) {
		var reportFile = tempDir.resolve("report.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--format=JSON",
			"--report=" + reportFile);

		assertThat(reportFile).isNotEmptyFile();
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void write_report_markdown(@TempDir Path tempDir) {
		var reportFile = tempDir.resolve("report.md");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--format=MD",
			"--report=" + reportFile);

		assertThat(reportFile).isNotEmptyFile();
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void write_report_io_error(@TempDir Path tempDir) throws IOException {
		var reportFile = tempDir.resolve("report.csv");
		Files.writeString(reportFile, "");
		reportFile.toFile().setReadOnly();

		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--report=" + reportFile,
			"--format=CSV");

		assertThat(err.toString()).contains("Error writing report to " + reportFile);
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	// --- Ignored --- //
	@Test
	void ignore_simple_bc(@TempDir Path tempDir) throws IOException {
		var ignored = tempDir.resolve("ignored.csv");
		Files.writeString(ignored, """
			type;symbol;kind;nature;location
			pkg.T;pkg.T.m();METHOD_REMOVED;DELETION;pkg/T.java:10""");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--ignored=" + ignored,
			"--plain");

		assertThat(out.toString()).doesNotContain("METHOD_REMOVED pkg.T.m");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void ignore_unknown_bc(@TempDir Path tempDir) throws IOException {
		var ignored = tempDir.resolve("ignored.csv");
		Files.writeString(ignored, """
			type;symbol;kind;nature;location
			pkg.T;pkg.T.m2();METHOD_REMOVED;DELETION;pkg/T.java:10""");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--ignored=" + ignored,
			"--plain");

		assertThat(out.toString()).contains("pkg.T.m() METHOD_REMOVED");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void ignore_all_bcs_in_report(@TempDir Path tempDir) {
		var ignored = tempDir.resolve("ignored.csv");
		cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--report=" + ignored,
			"--format=CSV");

		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--ignored=" + ignored,
			"--plain");

		assertThat(out.toString()).contains("No breaking changes found.");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void minimal_ignored_file(@TempDir Path tempDir) throws IOException {
		var ignored = tempDir.resolve("ignored.csv");
		Files.writeString(ignored, """
			type;symbol;kind
			pkg.T;pkg.T.m();METHOD_REMOVED
			pkg.T;pkg.T;TYPE_FORMAL_TYPE_PARAMETERS_REMOVED""");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--ignored=" + ignored,
			"--plain");

		assertThat(out.toString())
			.doesNotContain("METHOD_REMOVED")
			.doesNotContain("TYPE_FORMAL_TYPE_PARAMETERS_REMOVED")
			.contains("METHOD_NOW_STATIC");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void invalid_ignored_csv(@TempDir Path tempDir) {
		var invalidCsv = tempDir.resolve("nonexistent.csv");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--ignored=" + invalidCsv);

		assertThat(err.toString()).contains("Cannot find ignored CSV: " + invalidCsv);
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	@Test
	void malformed_ignored_csv(@TempDir Path tempDir) throws IOException {
		var malformedCsv = tempDir.resolve("malformed.csv");
		Files.writeString(malformedCsv, """
			type;symbol;kind;nature;location
			one_field;a_second_field""");

		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--ignored=" + malformedCsv,
			"--plain");

		assertThat(err.toString()).contains("Malformed line");
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	@Test
	void malformed_ignored_csv_kind(@TempDir Path tempDir) throws IOException {
		var ignored = tempDir.resolve("ignored.csv");
		Files.writeString(ignored, """
			type;symbol;kind;nature;location
			pkg.T;pkg.T.m();UNKNOWN;UNKNOWN;UNKNOWN""");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--ignored=" + ignored,
			"--plain",
			"-vv");

		assertThat(err.toString()).contains("Malformed kind");
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	// --- Config --- //
	@Test
	void warning_on_missing_config() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--config=nonexistent.yaml");

		assertThat(err.toString()).contains("Warning: ignoring missing configuration file");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void verbose_mode_with_exception() {
		var exitCode = cmd.execute("--v1=src/test/resources/invalid-path",
			"--api",
			"--verbose");

		assertThat(err.toString())
			.contains("Cannot find v1:")
			.contains("io.github.alien.roseau.RoseauException");
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}

	@Test
	void plain_mode_formatting() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--plain");

		assertThat(out.toString()).doesNotContain("\u001B[");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void colored_output_formatting() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(out.toString()).contains("\u001B[");
		assertThat(exitCode).isEqualTo(ExitCode.SUCCESS.code());
	}

	@Test
	void corrupt_jar() {
		var exitCode = cmd.execute("--v1=src/test/resources/corrupt.jar",
			"--v2=src/test/resources/corrupt.jar",
			"--diff");

		assertThat(err.toString()).contains("Invalid path to library");
		assertThat(exitCode).isEqualTo(ExitCode.ERROR.code());
	}
}
