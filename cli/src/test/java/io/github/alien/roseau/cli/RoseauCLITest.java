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

	@Test
	void no_mode() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src");

		assertThat(exitCode).isEqualTo(2);
		assertThat(err.toString()).contains("No mode selected: see --api or --diff");
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
	void write_api_default_sources() throws Exception {
		var json = new File("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--verbose");

		assertThat(exitCode).isZero();
		assertThat(json).isFile().isNotEmpty();
		assertThat(out.toString())
			.contains("Extracting API from", "using JDT")
			.contains("API has been written to api.json");

		Files.deleteIfExists(json.toPath());
	}

	@Test
	void write_api_default_jar() throws Exception {
		var json = new File("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--api",
			"--verbose");

		assertThat(exitCode).isZero();
		assertThat(json).isFile().isNotEmpty();
		assertThat(out.toString())
			.contains("Extracting API from", "using ASM")
			.contains("API has been written to api.json");

		Files.deleteIfExists(json.toPath());
	}

	@Test
	void write_api_custom_file() throws Exception {
		var json = new File("out.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--json=" + json);

		assertThat(exitCode).isZero();
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

		assertThat(exitCode).isEqualTo(2);
		assertThat(json).doesNotExist();
	}

	@Test
	void write_api_asm() throws Exception {
		var json = new File("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
			"--extractor=ASM",
			"--api",
			"--verbose");

		assertThat(exitCode).isZero();
		assertThat(json).isFile().isNotEmpty();
		assertThat(out.toString())
			.contains("Extracting API from", "using ASM")
			.contains("API has been written to api.json");

		Files.deleteIfExists(json.toPath());
	}

	@Test
	void write_api_spoon() throws Exception {
		var json = new File("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--extractor=SPOON",
			"--api",
			"--verbose");

		assertThat(exitCode).isZero();
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

		assertThat(exitCode).isEqualTo(2);
	}

	@Test
	void missing_classpath() throws Exception {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Warning: no classpath provided, results may be inaccurate");
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
	void write_report() throws Exception {
		var reportFile = new File("out.csv");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--report=" + reportFile.getPath());

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Report has been written to out.csv");
		assertThat(reportFile).isFile().isNotEmpty();

		Files.deleteIfExists(reportFile.toPath());
	}

	@Test
	void write_report_html() throws Exception {
		var reportFile = new File("report.html");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--format=HTML",
			"--report=" + reportFile.getPath());

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Report has been written to report.html");
		assertThat(reportFile).isFile().isNotEmpty();

		Files.deleteIfExists(reportFile.toPath());
	}

	@Test
	void write_report_json() throws Exception {
		var reportFile = new File("report.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--format=JSON",
			"--report=" + reportFile.getPath());

		assertThat(exitCode).isZero();
		assertThat(out.toString()).contains("Report has been written to report.json");
		assertThat(reportFile).isFile().isNotEmpty();

		Files.deleteIfExists(reportFile.toPath());
	}
}
