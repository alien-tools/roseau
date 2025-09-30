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

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("Missing required argument (specify one of these): (--api | --diff)");
	}

	// --- Diffs --- //
	@Test
	void simple_source_diff() throws Exception {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("METHOD_REMOVED pkg.T.m");
	}

	@Test
	void simple_jar_diff() throws Exception {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("METHOD_REMOVED pkg.T.m");
	}

	@Test
	void heterogeneous_diff_1() throws Exception {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("METHOD_REMOVED pkg.T.m");
	}

	@Test
	void heterogeneous_diff_2() throws Exception {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v1/src",
			"--diff",
			"--plain");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("No breaking changes found.");
	}

	@Test
	void no_breaking_changes() throws Exception {
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
	}

	@Test
	void missing_v2_in_diff() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--diff");

		assertThat(exitCode).isEqualTo(2);
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
	void write_api_default_sources(@TempDir Path tempDir) throws Exception {
		var jsonFile = tempDir.resolve("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--json=" + jsonFile,
			"--verbose");

		assertThat(exitCode).isZero();
		assertThat(jsonFile).isNotEmptyFile();
		assertThat(out.toString())
			.contains("Extracting API from", "using JDT")
			.contains("API has been written to " + jsonFile);
	}

	@Test
	void write_api_default_jar(@TempDir Path tempDir) throws Exception {
		var jsonFile = tempDir.resolve("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--api",
			"--json=" + jsonFile,
			"--verbose");

		assertThat(exitCode).isZero();
		assertThat(jsonFile).isNotEmptyFile();
		assertThat(out.toString())
			.contains("Extracting API from", "using ASM")
			.contains("API has been written to " + jsonFile);
	}

	@Test
	void write_api_custom_file(@TempDir Path tempDir) throws Exception {
		var jsonFile = tempDir.resolve("out.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--json=" + jsonFile);

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("API has been written to " + jsonFile);
		assertThat(jsonFile).isNotEmptyFile();
	}

	@Test
	void write_api_incorrect_extractor_asm(@TempDir Path tempDir) {
		var json = tempDir.resolve("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--extractor=ASM");

		assertThat(exitCode).isEqualTo(2);
		assertThat(json).doesNotExist();
	}

	@Test
	void write_api_asm(@TempDir Path tempDir) throws Exception {
		var jsonFile = tempDir.resolve("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--extractor=ASM",
			"--api",
			"--json=" + jsonFile,
			"--verbose");

		assertThat(exitCode).isZero();
		assertThat(jsonFile).isNotEmptyFile();
		assertThat(out.toString())
			.contains("Extracting API from", "using ASM")
			.contains("API has been written to " + jsonFile);
	}

	@Test
	void write_api_spoon(@TempDir Path tempDir) throws Exception {
		var jsonFile = tempDir.resolve("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--extractor=SPOON",
			"--api",
			"--json=" + jsonFile,
			"--verbose");

		assertThat(exitCode).isZero();
		assertThat(jsonFile).isNotEmptyFile();
		assertThat(out.toString())
			.contains("Extracting API from", "using SPOON")
			.contains("API has been written to " + jsonFile);
	}

	// --- Options --- //
	@Test
	void missing_v1() {
		var exitCode = cmd.execute("--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(exitCode).isEqualTo(2);
	}

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
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--classpath=src/test/resources/test-project-v1/test-project-v1.jar" +
				":src/test/resources/test-project-v2/test-project-v2.jar" +
				":src/test/resources/test-project-v1/test-project-v1.jar",
			"--verbose");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Classpath: [src/test/resources/test-project-v2/test-project-v2.jar, " +
			"src/test/resources/test-project-v1/test-project-v1.jar]");
	}

	@Test
	void invalid_pom() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--pom=src/test/resources/none.xml");

		assertThat(exitCode).isEqualTo(2);
	}

	@Test
	void unsupported_extractor() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--extractor=UNKNOWN",
			"--api");

		assertThat(exitCode).isEqualTo(2);
	}

	@Test
	void unsupported_formatter() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--formatter=UNKNOWN",
			"--diff");

		assertThat(exitCode).isEqualTo(2);
	}

	// --- Reports --- //
	@Test
	void write_report(@TempDir Path tempDir) throws Exception {
		var reportFile = tempDir.resolve("out.csv");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--report=" + reportFile);

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Report has been written to " + reportFile);
		assertThat(reportFile).isNotEmptyFile();
	}

	@Test
	void write_report_html(@TempDir Path tempDir) throws Exception {
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
	void write_report_json(@TempDir Path tempDir) throws Exception {
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
		assertThat(out.toString()).doesNotContain("METHOD_REMOVED pkg.T.m");
	}
}
