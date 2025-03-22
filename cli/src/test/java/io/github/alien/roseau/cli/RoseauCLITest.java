package io.github.alien.roseau.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.io.FileMatchers.aFileWithSize;
import static org.hamcrest.io.FileMatchers.anExistingFile;

class RoseauCLITest {
	RoseauCLI app;
	CommandLine cmd;

	@BeforeEach
	void setUp() {
		app = new RoseauCLI();
		cmd = new CommandLine(app);
	}

	// --- Diffs --- //
	@Test
	void simple_diff() throws Exception {
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--v2=src/test/resources/test-project-v2/src",
				"--diff",
				"--plain")
		);

		assertThat(out, containsString("METHOD_REMOVED pkg.T.m"));
	}

	@Test
	void heterogeneous_diff_1() throws Exception {
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--v2=src/test/resources/test-project-v2/test-project-v2.jar",
				"--diff",
				"--plain")
		);

		assertThat(out, containsString("METHOD_REMOVED pkg.T.m"));
	}

	@Test
	void heterogeneous_diff_2() throws Exception {
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
				"--v2=src/test/resources/test-project-v1/src",
				"--diff",
				"--plain")
		);

		assertThat(out, containsString("No breaking changes found."));
	}

	@Test
	void no_breaking_changes() throws Exception {
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--v2=src/test/resources/test-project-v1/src",
				"--diff")
		);

		assertThat(out, containsString("No breaking changes found."));
	}

	@Test
	void invalid_v1_path() throws Exception {
		var exitCode = cmd.execute("--v1=src/test/resources/invalid-path",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(exitCode, is(not(0)));
	}

	@Test
	void missing_v2_in_diff() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--diff");
		assertThat(exitCode, is(not(0)));
	}

	@Test
	void fail_mode_with_breaking_changes() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--fail");

		assertThat(exitCode, is(1));
	}

	@Test
	void no_fail_mode_with_breaking_changes() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff");

		assertThat(exitCode, is(0));
	}

	// --- APIs --- //
	@Test
	void write_api_default_sources() throws Exception {
		var json = new File("api.json");
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--api",
				"--verbose")
		);

		assertThat(json, aFileWithSize(greaterThan(1L)));
		assertThat(out, containsString(
			"Extracting API from sources src/test/resources/test-project-v1/src using JDT"));
		assertThat(out, containsString("Wrote API to api.json"));

		Files.deleteIfExists(json.toPath());
	}

	@Test
	void write_api_default_jar() throws Exception {
		var json = new File("api.json");
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
				"--api",
				"--verbose")
		);

		assertThat(json, aFileWithSize(greaterThan(1L)));
		assertThat(out, containsString(
			"Extracting API from sources src/test/resources/test-project-v1/test-project-v1.jar using ASM"));
		assertThat(out, containsString("Wrote API to api.json"));

		Files.deleteIfExists(json.toPath());
	}

	@Test
	void write_api_custom_file() throws Exception {
		var json = new File("out.json");
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--api",
				"--json=" + json,
				"--verbose")
		);

		assertThat(out, containsString("Wrote API to out.json"));
		assertThat(json, aFileWithSize(greaterThan(1L)));

		Files.deleteIfExists(json.toPath());
	}

	@Test
	void write_api_incorrect_extractor_asm() {
		var json = new File("api.json");
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--extractor=ASM",
			"--verbose");

		assertThat(exitCode, is(not(0)));
		assertThat(json, not(anExistingFile()));
	}

	@Test
	void write_api_asm() throws Exception {
		var json = new File("api.json");
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/test-project-v1.jar",
				"--extractor=ASM",
				"--api",
				"--verbose")
		);

		assertThat(json, aFileWithSize(greaterThan(1L)));
		assertThat(out, containsString(
			"Extracting API from sources src/test/resources/test-project-v1/test-project-v1.jar using ASM"));
		assertThat(out, containsString("Wrote API to api.json"));

		Files.deleteIfExists(json.toPath());
	}

	@Test
	void write_api_spoon() throws Exception {
		var json = new File("api.json");
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--extractor=SPOON",
				"--api",
				"--verbose")
		);

		assertThat(json, aFileWithSize(greaterThan(1L)));
		assertThat(out, containsString(
			"Extracting API from sources src/test/resources/test-project-v1/src using Spoon"));
		assertThat(out, containsString("Wrote API to api.json"));

		Files.deleteIfExists(json.toPath());
	}

	// --- Options --- //
	@Test
	void missing_v1() {
		var exitCode = cmd.execute("--v2=src/test/resources/test-project-v2/src",
			"--diff");
		assertThat(exitCode, is(not(0)));
	}

	@Test
	void missing_classpath() throws Exception {
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--v2=src/test/resources/test-project-v2/src",
				"--diff")
		);

		assertThat(out, containsString("No classpath provided, results may be inaccurate"));
	}

	@Test
	void invalid_pom() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--api",
			"--pom=src/test/resources/none.xml");
		assertThat(exitCode, is(not(0)));
	}

	@Test
	void unsupported_extractor() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--extractor=UNKNOWN",
			"--api");

		assertThat(exitCode, is(not(0)));
	}

	@Test
	void unsupported_formatter() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--formatter=UNKNOWN",
			"--diff");

		assertThat(exitCode, is(not(0)));
	}

	// --- Reports --- //
	@Test
	void write_report() throws Exception {
		var reportFile = new File("out.csv");
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--v2=src/test/resources/test-project-v2/src",
				"--diff",
				"--report=" + reportFile.getPath(),
				"--verbose")
		);

		assertThat(out, containsString("Wrote report to out.csv"));
		assertThat(reportFile, aFileWithSize(greaterThan(1L)));

		Files.deleteIfExists(reportFile.toPath());
	}

	@Test
	void write_report_html() throws Exception {
		var reportFile = new File("report.html");
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--v2=src/test/resources/test-project-v2/src",
				"--diff",
				"--format=HTML",
				"--report=" + reportFile.getPath(),
				"--verbose")
		);

		assertThat(out, containsString("Wrote report to report.html"));
		assertThat(reportFile, aFileWithSize(greaterThan(1L)));

		Files.deleteIfExists(reportFile.toPath());
	}

	@Test
	void write_report_json() throws Exception {
		var reportFile = new File("report.json");
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--v2=src/test/resources/test-project-v2/src",
				"--diff",
				"--format=JSON",
				"--report=" + reportFile.getPath(),
				"--verbose")
		);

		assertThat(out, containsString("Wrote report to report.json"));
		assertThat(reportFile, aFileWithSize(greaterThan(1L)));

		Files.deleteIfExists(reportFile.toPath());
	}
}
