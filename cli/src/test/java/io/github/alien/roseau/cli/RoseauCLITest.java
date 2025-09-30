package io.github.alien.roseau.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;

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

	// --- Diffs --- //
	@Test
	void simple_source_diff() throws Exception {
		cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--plain");

		assertThat(out.toString()).contains("METHOD_REMOVED pkg.T.m");
	}

	@Test
	void simple_jar_diff() throws Exception {
		cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--plain");

		assertThat(out.toString()).contains("METHOD_REMOVED pkg.T.m");
	}

	@Test
	void heterogeneous_diff_1() throws Exception {
		cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
			"--diff",
			"--plain");

		assertThat(out.toString()).contains("METHOD_REMOVED pkg.T.m");
	}

	@Test
	void heterogeneous_diff_2() throws Exception {
		cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v1/src",
			"--diff",
			"--plain");

		assertThat(out.toString()).contains("No breaking changes found.");
	}

	@Test
	void no_breaking_changes() throws Exception {
		cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--v2=src/test/resources/test-project-v1/test-project-v1.jar",
			"--diff");

		assertThat(out.toString()).contains("No breaking changes found.");
	}

	@Test
	void invalid_v1_path() {
		var exitCode = cmd.execute("--v1=src/test/resources/invalid-path",
			"--v2=src/test/resources/test-project-v2/test-project-v1.jar",
			"--diff");

		assertThat(exitCode).isNotZero();
	}

	@Test
	void missing_v2_in_diff() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--diff");

		assertThat(exitCode).isNotZero();
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
	void write_api_default_sources() throws Exception {
		var json = new File("api.json");
		cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--verbose");

		assertThat(json).isFile().isNotEmpty();
		assertThat(out.toString())
			.contains("Extracting API from", "using JDT")
			.contains("API has been written to api.json");

		Files.deleteIfExists(json.toPath());
	}

	@Test
	void write_api_default_jar() throws Exception {
		var json = new File("api.json");
		cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--api",
			"--verbose");

		assertThat(json).isFile().isNotEmpty();
		assertThat(out.toString())
			.contains("Extracting API from", "using ASM")
			.contains("API has been written to api.json");

		Files.deleteIfExists(json.toPath());
	}

	@Test
	void write_api_custom_file() throws Exception {
		var json = new File("out.json");
		cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--json=" + json);

		assertThat(out.toString()).contains("API has been written to out.json");
		assertThat(json).isFile().isNotEmpty();

		Files.deleteIfExists(json.toPath());
	}

	@Test
	void write_api_incorrect_extractor_asm() {
		var json = new File("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--extractor=ASM");

		assertThat(exitCode).isNotZero();
		assertThat(json).doesNotExist();
	}

	@Test
	void write_api_asm() throws Exception {
		var json = new File("api.json");
		cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--extractor=ASM",
			"--api",
			"--verbose");

		assertThat(json).isFile().isNotEmpty();
		assertThat(out.toString())
			.contains("Extracting API from", "using ASM")
			.contains("API has been written to api.json");

		Files.deleteIfExists(json.toPath());
	}

	@Test
	void write_api_spoon() throws Exception {
		var json = new File("api.json");
		cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--extractor=SPOON",
			"--api",
			"--verbose");

		assertThat(json).isFile().isNotEmpty();
		assertThat(out.toString())
			.contains("Extracting API from", "using SPOON")
			.contains("API has been written to api.json");

		Files.deleteIfExists(json.toPath());
	}

	// --- Options --- //
	@Test
	void missing_v1() {
		var exitCode = cmd.execute("--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(exitCode).isNotZero();
	}

	@Test
	void missing_classpath() throws Exception {
		cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(out.toString()).contains("Warning: no classpath provided, results may be inaccurate");
	}

	@Test
	void invalid_pom() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--pom=src/test/resources/none.xml");

		assertThat(exitCode).isNotZero();
	}

	@Test
	void unsupported_extractor() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--extractor=UNKNOWN",
			"--api");

		assertThat(exitCode).isNotZero();
	}

	@Test
	void unsupported_formatter() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--formatter=UNKNOWN",
			"--diff");

		assertThat(exitCode).isNotZero();
	}

	// --- Reports --- //
	@Test
	void write_report() throws Exception {
		var reportFile = new File("out.csv");
		cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--report=" + reportFile.getPath());

		assertThat(out.toString()).contains("Report has been written to out.csv");
		assertThat(reportFile).isFile().isNotEmpty();

		Files.deleteIfExists(reportFile.toPath());
	}

	@Test
	void write_report_html() throws Exception {
		var reportFile = new File("report.html");
		cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--format=HTML",
			"--report=" + reportFile.getPath());

		assertThat(out.toString()).contains("Report has been written to report.html");
		assertThat(reportFile).isFile().isNotEmpty();

		Files.deleteIfExists(reportFile.toPath());
	}

	@Test
	void write_report_json() throws Exception {
		var reportFile = new File("report.json");
		cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--format=JSON",
			"--report=" + reportFile.getPath());

		assertThat(out.toString()).contains("Report has been written to report.json");
		assertThat(reportFile).isFile().isNotEmpty();

		Files.deleteIfExists(reportFile.toPath());
	}
}
