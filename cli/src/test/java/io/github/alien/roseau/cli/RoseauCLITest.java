package io.github.alien.roseau.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.io.FileMatchers.aFileWithSize;

class RoseauCLITest {
	RoseauCLI app;
	CommandLine cmd;

	@BeforeEach
	void setUp() {
		app = new RoseauCLI();
		cmd = new CommandLine(app);
	}

	@AfterEach
	void tearDown() throws Exception {
		Files.deleteIfExists(Path.of("api.json"));
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
	void no_breaking_changes() throws Exception {
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--v2=src/test/resources/test-project-v1/src",
				"--diff",
				"--plain")
		);

		assertThat(out, containsString("No breaking changes found."));
	}

	@Test
	void invalid_v1_path() throws Exception {
		var exitCode = cmd.execute("--v1=src/test/resources/invalid-path",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--plain");

		assertThat(exitCode, is(not(0)));
	}

	@Test
	void missing_v2_in_diff() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src", "--diff");
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
	void write_api_default() throws Exception {
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--api",
				"--verbose")
		);

		assertThat(out, containsString("Wrote API to api.json"));
	}

	@Test
	void write_api_custom() throws Exception {
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

	// --- Options --- //
	@Test
	void missing_v1_option() {
		var exitCode = cmd.execute("--v2=src/test/resources/test-project-v2/src", "--diff");
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
	void unsupported_extractor() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--extractor=UNKNOWN",
			"--api");

		assertThat(exitCode, is(not(0)));
	}

	// --- Reports --- //
	@Test
	void generate_report() throws Exception {
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
	void write_report_csv() throws Exception {
		var reportFile = new File("report.csv");
		var out = tapSystemOutNormalized(() ->
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--v2=src/test/resources/test-project-v2/src",
				"--diff",
				"--format=CSV",
				"--report=" + reportFile.getPath(),
				"--verbose")
		);

		assertThat(out, containsString("Wrote report to report.csv"));
		assertThat(reportFile, aFileWithSize(greaterThan(1L)));

		Files.deleteIfExists(reportFile.toPath());
	}

	@Test
	void invalid_report_format() {
		var exitCode = cmd.execute("--v1=src/test/resources/test-project-v1/src",
			"--v2=src/test/resources/test-project-v2/src",
			"--diff",
			"--format=UNKNOWN");

		assertThat(exitCode, is(not(0)));
	}
}
