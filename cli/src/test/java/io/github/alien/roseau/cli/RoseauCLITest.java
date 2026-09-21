package io.github.alien.roseau.cli;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static io.github.alien.roseau.cli.CliResultAssert.assertThatCli;
import static io.github.alien.roseau.cli.CliRunner.run;
import static io.github.alien.roseau.cli.CliRunner.runExpectingDiagnostics;
import static org.assertj.core.api.Assertions.assertThat;

class RoseauCLITest {
	private static final String V1_SOURCES = "--v1=src/test/resources/test-project-v1/src";
	private static final String V2_SOURCES = "--v2=src/test/resources/test-project-v2/src";
	private static final String V1_JAR = "--v1=src/test/resources/test-project-v1/test-project-v1.jar";
	private static final String V2_JAR = "--v2=src/test/resources/test-project-v2/test-project-v2.jar";

	private static final String FULL_DIFF = """
		Breaking Changes found: 4 (2 binary-breaking, 3 source-breaking)
		⚠ pkg.T FORMAL_TYPE_PARAMETER_REMOVED [U]
		  ✓ binary-compatible ✗ source-breaking
		  → pkg/T.java:3
		✗ pkg.T.m() EXECUTABLE_REMOVED
		  ✗ binary-breaking ✗ source-breaking
		  → pkg/T.java:4
		⚠ pkg.T.n() METHOD_NOW_STATIC
		  ✗ binary-breaking ✓ source-compatible
		  → pkg/T.java:5
		⚠ pkg.T.n() METHOD_OVERRIDABLE_NOW_STATIC
		  ✓ binary-compatible ✗ source-breaking
		  → pkg/T.java:5
		""";

	private static final String FULL_DIFF_FROM_JAR = FULL_DIFF.replace("→ pkg/T.java:3", "→ pkg/T.java");

	private static final String NO_CHANGES = "No breaking changes found.";

	@Nested
	class Meta {
		@Test
		void help_documents_every_option() throws IOException {
			String expected = Files.readString(Path.of("src/test/resources/expected/help.txt"));
			assertThatCli(run("--help"))
				.succeeded()
				.hasStdout(expected);
		}

		@Test
		void version_reports_the_build_version() {
			assertThatCli(run("--version"))
				.succeeded()
				.hasStdout("Roseau " + RoseauCLI.VersionProvider.resolveVersion() + System.lineSeparator());
		}

		@Test
		void version_is_resolved_rather_than_unknown() {
			assertThat(RoseauCLI.VersionProvider.resolveVersion())
				.isNotEqualTo("unknown");
		}

		@Test
		void a_mode_is_required() {
			var result = runExpectingDiagnostics(V1_SOURCES);

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.reportedError("Error: Missing required argument (specify one of these): (--api | --diff)")
				.stderrContains("Usage: roseau");
		}

		@Test
		void unknown_options_are_rejected() {
			var result = runExpectingDiagnostics(V1_SOURCES, V2_SOURCES, "--formatter=UNKNOWN", "--diff");

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.reportedError("Unknown option: '--formatter=UNKNOWN'");
		}

		@Test
		void source_only_and_binary_only_are_mutually_exclusive() {
			var result = runExpectingDiagnostics(V1_JAR, V2_JAR, "--diff", "--source-only", "--binary-only");

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.hasStderr("Specify either --source-only or --binary-only" + System.lineSeparator()
					+ "Use -v/-vv for detailed error logs." + System.lineSeparator());
		}
	}

	@Nested
	class Diffs {
		@Test
		void sources_against_sources() {
			assertThatCli(run(V1_SOURCES, V2_SOURCES, "--diff", "--plain"))
				.succeeded()
				.hasReport(FULL_DIFF);
		}

		@Test
		void jar_against_jar() {
			assertThatCli(run(V1_JAR, V2_JAR, "--diff", "--plain"))
				.succeeded()
				.hasReport(FULL_DIFF_FROM_JAR);
		}

		@Test
		void sources_against_jar() {
			assertThatCli(run(V1_SOURCES, V2_JAR, "--diff", "--plain"))
				.succeeded()
				.hasReport(FULL_DIFF);
		}

		@Test
		void jar_against_sources() {
			assertThatCli(run(V1_JAR, V2_SOURCES, "--diff", "--plain"))
				.succeeded()
				.hasReport(FULL_DIFF_FROM_JAR);
		}

		@Test
		void identical_versions_report_nothing() {
			assertThatCli(run(V1_JAR, "--v2=src/test/resources/test-project-v1/test-project-v1.jar", "--diff"))
				.succeeded()
				.hasReport(NO_CHANGES);
		}

		@Test
		void inherited_members_name_the_type_they_impact() {
			assertThatCli(run("--v1=src/test/resources/inheritance-v1/src",
				"--v2=src/test/resources/inheritance-v2/src", "--diff", "--plain"))
				.succeeded()
				.hasReport("""
					Breaking Changes found: 2 (2 binary-breaking, 2 source-breaking)
					✗ pkg.Base.removed() EXECUTABLE_REMOVED
					  ✗ binary-breaking ✗ source-breaking
					  → pkg/Base.java:5
					✗ pkg.Base.removed() in pkg.Child EXECUTABLE_REMOVED
					  ✗ binary-breaking ✗ source-breaking
					  → pkg/Base.java:5
					""");
		}

		@Test
		void binary_only_keeps_the_binary_breaking_changes() {
			assertThatCli(run(V1_JAR, V2_JAR, "--diff", "--binary-only", "--plain"))
				.succeeded()
				.hasReport("""
					Breaking Changes found: 2 (2 binary-breaking, 1 source-breaking)
					✗ pkg.T.m() EXECUTABLE_REMOVED
					  ✗ binary-breaking ✗ source-breaking
					  → pkg/T.java:4
					⚠ pkg.T.n() METHOD_NOW_STATIC
					  ✗ binary-breaking ✓ source-compatible
					  → pkg/T.java:5
					""");
		}

		@Test
		void source_only_keeps_the_source_breaking_changes() {
			assertThatCli(run(V1_JAR, V2_JAR, "--diff", "--source-only", "--plain"))
				.succeeded()
				.hasReport("""
					Breaking Changes found: 3 (1 binary-breaking, 3 source-breaking)
					⚠ pkg.T FORMAL_TYPE_PARAMETER_REMOVED [U]
					  ✓ binary-compatible ✗ source-breaking
					  → pkg/T.java
					✗ pkg.T.m() EXECUTABLE_REMOVED
					  ✗ binary-breaking ✗ source-breaking
					  → pkg/T.java:4
					⚠ pkg.T.n() METHOD_OVERRIDABLE_NOW_STATIC
					  ✓ binary-compatible ✗ source-breaking
					  → pkg/T.java:5
					""");
		}

		@Test
		void module_exports_restrict_the_api() {
			assertThatCli(run("--v1=src/test/resources/module-library-v1/src",
				"--v2=src/test/resources/module-library-v2/src", "--diff", "--plain"))
				.succeeded()
				.hasReport("""
					Breaking Changes found: 1 (1 binary-breaking, 1 source-breaking)
					✗ exported.Exported.m() EXECUTABLE_REMOVED
					  ✗ binary-breaking ✗ source-breaking
					  → exported/Exported.java:4
					""");
		}

		@Test
		void ignore_module_takes_the_class_path_clients_view() {
			assertThatCli(run("--v1=src/test/resources/module-library-v1/src",
				"--v2=src/test/resources/module-library-v2/src", "--diff", "--ignore-module", "--plain"))
				.succeeded()
				.hasReport("""
					Breaking Changes found: 2 (2 binary-breaking, 2 source-breaking)
					✗ exported.Exported.m() EXECUTABLE_REMOVED
					  ✗ binary-breaking ✗ source-breaking
					  → exported/Exported.java:4
					✗ internal.Internal.m() EXECUTABLE_REMOVED
					  ✗ binary-breaking ✗ source-breaking
					  → internal/Internal.java:4
					""");
		}

		@Test
		void colors_only_wrap_the_plain_report() {
			var colored = run(V1_SOURCES, V2_SOURCES, "--diff");
			var plain = run(V1_SOURCES, V2_SOURCES, "--diff", "--plain");

			assertThatCli(colored)
				.succeeded()
				.stdoutContains("[");
			assertThat(colored.stdout().replaceAll("\\[[0-9;]*m", ""))
				.isEqualTo(plain.stdout());
		}

		@Test
		void plain_disables_colors() {
			assertThatCli(run(V1_SOURCES, V2_SOURCES, "--diff", "--plain"))
				.stdoutDoesNotContain("[");
		}

		@Test
		void corrupt_archives_are_rejected() {
			var result = runExpectingDiagnostics("--v1=src/test/resources/corrupt.jar",
				"--v2=src/test/resources/corrupt.jar", "--diff");

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.reportedError("Invalid path to library; directory or JAR expected: src/test/resources/corrupt.jar");
		}

		@Test
		void diff_requires_v2() {
			var result = runExpectingDiagnostics(V1_SOURCES, "--diff");

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.reportedError("Cannot find v2: null");
		}
	}

	@Nested
	class ExitCodes {
		@Test
		void breaking_changes_alone_do_not_fail_the_build() {
			assertThatCli(run(V1_SOURCES, V2_SOURCES, "--diff", "--plain"))
				.succeeded();
		}

		@Test
		void fail_on_bc_turns_breaking_changes_into_a_failure() {
			assertThatCli(run(V1_SOURCES, V2_SOURCES, "--diff", "--plain", "--fail-on-bc"))
				.hasBreakingChanges()
				.hasReport(FULL_DIFF);
		}

		@Test
		void fail_on_bc_succeeds_when_nothing_broke() {
			assertThatCli(run(V1_JAR, "--v2=src/test/resources/test-project-v1/test-project-v1.jar", "--diff",
				"--fail-on-bc"))
				.succeeded()
				.hasReport(NO_CHANGES);
		}
	}

	@Nested
	class ApiMode {
		@Test
		void api_is_printed_to_stdout_as_json() {
			var api = assertThatCli(run(V1_SOURCES, "--api"))
				.succeeded()
				.stdoutAsJson();

			assertThat(api.keySet()).contains("allTypes");
			assertThat(api.getJSONArray("allTypes")).hasSize(1);
		}

		@Test
		void api_is_written_to_the_requested_file(@TempDir Path tempDir) throws IOException {
			var json = tempDir.resolve("custom.json");

			assertThatCli(run(V1_JAR, "--api", "--api-json=" + json))
				.succeeded()
				.hasNoStdout();
			assertThat(new JSONObject(Files.readString(json)).getJSONArray("allTypes")).hasSize(1);
		}

		@Test
		void unwritable_destinations_fail(@TempDir Path tempDir) throws IOException {
			var json = tempDir.resolve("api.json");
			Files.writeString(json, "{}");
			assertThat(json.toFile().setReadOnly()).isTrue();

			var result = runExpectingDiagnostics(V1_SOURCES, "--api", "--api-json=" + json);
			assertThatCli(result)
				.failed()
				.reportedError("Error writing API to " + json);
		}

		@Test
		void api_mode_requires_v1() {
			var result = runExpectingDiagnostics("--api");

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.reportedError("Cannot find v1: null");
		}
	}

	@Nested
	class Classpaths {
		private static final String LIBRARY_JAR = "src/test/resources/classpath-library/classpath-library.jar";
		private static final String DEPENDENCY_JAR = "src/test/resources/classpath-dependency/classpath-dependency.jar";
		private static final String DEPENDENCY_CLASSES = "src/test/resources/classpath-dependency/classes";

		@Test
		void a_complete_classpath_is_quiet() {
			assertThatCli(run("--v1=" + LIBRARY_JAR, "--v2=" + LIBRARY_JAR, "--classpath=" + DEPENDENCY_CLASSES,
				"--diff", "--plain"))
				.succeeded()
				.hasReport(NO_CHANGES);
		}

		@Test
		void jars_and_class_directories_are_both_accepted() {
			assertThatCli(run("--v1=" + LIBRARY_JAR, "--v2=" + LIBRARY_JAR, "--v1-classpath=" + DEPENDENCY_JAR,
				"--v2-classpath=" + DEPENDENCY_CLASSES, "--diff", "--plain"))
				.succeeded()
				.hasReport(NO_CHANGES);
		}

		@Test
		void an_incomplete_classpath_warns_but_still_reports() {
			var result = runExpectingDiagnostics("--v1=" + LIBRARY_JAR, "--v2=" + LIBRARY_JAR, "--diff",
				"--plain");

			assertThatCli(result)
				.succeeded()
				.hasReport(NO_CHANGES)
				.hasNoStderr()
				.loggedWarning("1 type(s) could not be resolved (dependency.Base)");
		}

		@Test
		void fail_on_unresolved_aborts_before_printing_anything() {
			var result = runExpectingDiagnostics("--v1=" + LIBRARY_JAR, "--v2=" + LIBRARY_JAR, "--diff",
				"--fail-on-unresolved", "--plain");

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.reportedError("1 type(s) could not be resolved (dependency.Base); make sure the classpaths are accurate");
		}

		@Test
		void fail_on_unresolved_succeeds_with_a_complete_classpath() {
			assertThatCli(run("--v1=" + LIBRARY_JAR, "--v2=" + LIBRARY_JAR, "--classpath=" + DEPENDENCY_CLASSES,
				"--diff", "--fail-on-unresolved", "--plain"))
				.succeeded()
				.hasReport(NO_CHANGES);
		}

		@Test
		void a_valid_pom_supplies_the_classpath(@TempDir Path tempDir) {
			assertThatCli(run(V1_SOURCES, "--api", "--pom=src/test/resources/valid-pom.xml",
				"--api-json=" + tempDir.resolve("api.json")))
				.succeeded()
				.hasNoStdout();
		}

		@Test
		void a_corrupt_pom_warns_and_falls_back_to_an_empty_classpath(@TempDir Path tempDir) {
			var result = runExpectingDiagnostics(V1_SOURCES, "--api",
				"--pom=src/test/resources/corrupt-pom.xml", "--api-json=" + tempDir.resolve("api.json"));

			assertThatCli(result)
				.succeeded()
				.hasNoStderr()
				.loggedWarning("Failed to build Maven classpath from src/test/resources/corrupt-pom.xml");
		}

		@Test
		void a_missing_pom_is_an_error() {
			var result = runExpectingDiagnostics(V1_SOURCES, "--api", "--pom=src/test/resources/none.xml");

			assertThatCli(result)
				.failed()
				.reportedError("Cannot find pom: src/test/resources/none.xml");
		}
	}

	@Nested
	class Reports {
		@Test
		void csv_report_holds_one_row_per_change(@TempDir Path tempDir) throws IOException {
			var csv = tempDir.resolve("report.csv");

			assertThatCli(run(V1_SOURCES, V2_SOURCES, "--diff", "--plain", "--report=CSV=" + csv))
				.succeeded()
				.hasReport(FULL_DIFF);

			assertThat(CliResult.normalize(Files.readString(csv))).isEqualTo("""
				type;symbol;kind;nature;location;newSymbol;binaryBreaking;sourceBreaking
				pkg.T;pkg.T;FORMAL_TYPE_PARAMETER_REMOVED;MUTATION;pkg/T.java:3;;false;true
				pkg.T;pkg.T.m();EXECUTABLE_REMOVED;DELETION;pkg/T.java:4;;true;true
				pkg.T;pkg.T.n();METHOD_NOW_STATIC;MUTATION;pkg/T.java:5;pkg.T.n();true;false
				pkg.T;pkg.T.n();METHOD_OVERRIDABLE_NOW_STATIC;MUTATION;pkg/T.java:5;pkg.T.n();false;true""");
		}

		@Test
		void json_report_holds_one_object_per_change(@TempDir Path tempDir) throws IOException {
			var json = tempDir.resolve("report.json");

			run(V1_SOURCES, V2_SOURCES, "--diff", "--report=JSON=" + json);

			assertThat(new JSONArray(Files.readString(json))).hasSize(4);
		}

		@Test
		void markdown_report_holds_a_table(@TempDir Path tempDir) throws IOException {
			var md = tempDir.resolve("report.md");

			run(V1_SOURCES, V2_SOURCES, "--diff", "--report=MD=" + md);

			assertThat(Files.readString(md).lines().filter(line -> !line.isEmpty() && line.charAt(0) == '|'))
				.hasSize(2 + 4)
				.first(InstanceOfAssertFactories.STRING)
				.isEqualTo("| Type | Symbol | Kind | Nature | Location | New symbol | Binary | Source |");
		}

		@Test
		void cli_report_matches_what_plain_mode_prints(@TempDir Path tempDir) throws IOException {
			var txt = tempDir.resolve("report.txt");
			var result = run(V1_SOURCES, V2_SOURCES, "--diff", "--plain", "--report=CLI=" + txt);

			assertThat(CliResult.normalize(Files.readString(txt)) + "\n")
				.isEqualTo(CliResult.normalize(result.stdout()))
				.doesNotContain("[");
		}

		@Test
		void reports_are_repeatable(@TempDir Path tempDir) {
			var csv = tempDir.resolve("report.csv");
			var json = tempDir.resolve("report.json");
			var html = tempDir.resolve("report.html");

			assertThatCli(run(V1_SOURCES, V2_SOURCES, "--diff", "--plain", "--report=CSV=" + csv,
				"--report=JSON=" + json, "--report=HTML=" + html))
				.succeeded()
				.hasReport(FULL_DIFF);

			assertThat(csv).isNotEmptyFile();
			assertThat(json).isNotEmptyFile();
			assertThat(html).isNotEmptyFile();
		}

		@Test
		void report_without_a_format_is_rejected(@TempDir Path tempDir) {
			var csv = tempDir.resolve("report.csv");
			var result = runExpectingDiagnostics(V1_SOURCES, V2_SOURCES, "--diff", "--report=" + csv);

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.stderrContains("Expected FORMAT=PATH");
			assertThat(csv).doesNotExist();
		}

		@Test
		void unknown_report_formats_are_rejected_before_the_diff_runs(@TempDir Path tempDir) {
			var report = tempDir.resolve("report.unknown");
			var result = runExpectingDiagnostics(V1_SOURCES, V2_SOURCES, "--diff", "--report=UNKNOWN=" + report);

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.reportedError("Invalid value for option '--report' (<format=path>): Unknown report format: UNKNOWN");
			assertThat(report).doesNotExist();
		}

		@Test
		void unwritable_destinations_fail(@TempDir Path tempDir) throws IOException {
			var csv = tempDir.resolve("report.csv");
			Files.writeString(csv, "");
			assertThat(csv.toFile().setReadOnly()).isTrue();

			var result = runExpectingDiagnostics(V1_SOURCES, V2_SOURCES, "--diff", "--plain",
				"--report=CSV=" + csv);

			assertThatCli(result)
				.failed()
				.reportedError("Error writing report to " + csv);
		}
	}

	@Nested
	class IgnoredChanges {
		@Test
		void a_listed_change_is_dropped_from_the_report(@TempDir Path tempDir) throws IOException {
			var ignored = write(tempDir, """
				type;symbol;kind;nature;location
				pkg.T;pkg.T.m();EXECUTABLE_REMOVED;DELETION;pkg/T.java:10""");

			assertThatCli(run(V1_JAR, V2_JAR, "--diff", "--plain", "--ignored=" + ignored))
				.succeeded()
				.hasReport(FULL_DIFF_FROM_JAR.replace("""
						✗ pkg.T.m() EXECUTABLE_REMOVED
						  ✗ binary-breaking ✗ source-breaking
						  → pkg/T.java:4
						""", "")
					.replace("found: 4 (2 binary-breaking, 3 source-breaking)",
						"found: 3 (1 binary-breaking, 2 source-breaking)"));
		}

		@Test
		void the_three_leading_columns_are_enough(@TempDir Path tempDir) throws IOException {
			var ignored = write(tempDir, """
				type;symbol;kind
				pkg.T;pkg.T.m();EXECUTABLE_REMOVED
				pkg.T;pkg.T;FORMAL_TYPE_PARAMETER_REMOVED""");

			assertThatCli(run(V1_JAR, V2_JAR, "--diff", "--plain", "--ignored=" + ignored))
				.succeeded()
				.hasReport("""
					Breaking Changes found: 2 (1 binary-breaking, 1 source-breaking)
					⚠ pkg.T.n() METHOD_NOW_STATIC
					  ✗ binary-breaking ✓ source-compatible
					  → pkg/T.java:5
					⚠ pkg.T.n() METHOD_OVERRIDABLE_NOW_STATIC
					  ✓ binary-compatible ✗ source-breaking
					  → pkg/T.java:5
					""");
		}

		@Test
		void a_csv_report_can_be_reused_as_an_ignore_list(@TempDir Path tempDir) {
			var baseline = tempDir.resolve("baseline.csv");
			run(V1_JAR, V2_JAR, "--diff", "--report=CSV=" + baseline);

			assertThatCli(run(V1_JAR, V2_JAR, "--diff", "--plain", "--ignored=" + baseline))
				.succeeded()
				.hasReport(NO_CHANGES);
		}

		@Test
		void a_missing_ignore_list_is_an_error(@TempDir Path tempDir) {
			var missing = tempDir.resolve("nonexistent.csv");
			var result = runExpectingDiagnostics(V1_SOURCES, V2_SOURCES, "--diff", "--ignored=" + missing);

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.reportedError("Cannot find ignored CSV: " + missing);
		}

		@Test
		void a_truncated_line_is_an_error(@TempDir Path tempDir) throws IOException {
			var ignored = write(tempDir, """
				type;symbol;kind;nature;location
				one_field;a_second_field""");
			var result = runExpectingDiagnostics(V1_JAR, V2_JAR, "--diff", "--ignored=" + ignored);

			assertThatCli(result)
				.failed()
				.stderrContains("Malformed line 'one_field;a_second_field'");
		}

		@Test
		void an_unknown_kind_is_an_error(@TempDir Path tempDir) throws IOException {
			var ignored = write(tempDir, """
				type;symbol;kind;nature;location
				pkg.T;pkg.T.m();UNKNOWN;UNKNOWN;UNKNOWN""");
			var result = runExpectingDiagnostics(V1_JAR, V2_JAR, "--diff", "--ignored=" + ignored);

			assertThatCli(result)
				.failed()
				.stderrContains("Malformed kind 'UNKNOWN'");
		}

		private static Path write(Path tempDir, String contents) throws IOException {
			var ignored = tempDir.resolve("ignored.csv");
			Files.writeString(ignored, contents);
			return ignored;
		}
	}

	@Nested
	class Configuration {
		@Test
		void a_missing_config_file_warns_and_falls_back_to_defaults() {
			var result = runExpectingDiagnostics(V1_SOURCES, V2_SOURCES, "--diff", "--plain",
				"--config=nonexistent.yaml");

			assertThatCli(result)
				.failed()
				.stderrContains("Missing configuration file nonexistent.yaml" + System.lineSeparator());
		}

		@Test
		void yaml_source_only_is_honored(@TempDir Path tempDir) throws IOException {
			var config = write(tempDir, """
				diff:
				  sourceOnly: true
				""");

			assertThatCli(run(V1_SOURCES, V2_SOURCES, "--diff", "--plain", "--config=" + config))
				.succeeded()
				.stdoutContains("FORMAL_TYPE_PARAMETER_REMOVED")
				.stdoutDoesNotContain("pkg.T.n() METHOD_NOW_STATIC");
		}

		@Test
		void cli_flags_win_over_the_config_file(@TempDir Path tempDir) throws IOException {
			var config = write(tempDir, """
				diff:
				  sourceOnly: true
				""");

			assertThatCli(run(V1_SOURCES, V2_SOURCES, "--diff", "--plain", "--binary-only", "--config=" + config))
				.succeeded()
				.stdoutDoesNotContain("FORMAL_TYPE_PARAMETER_REMOVED")
				.stdoutContains("METHOD_NOW_STATIC");
		}

		@Test
		void yaml_ignore_module_is_honored(@TempDir Path tempDir) throws IOException {
			var config = write(tempDir, """
				common:
				  ignoreModule: true
				""");

			assertThatCli(run("--v1=src/test/resources/module-library-v1/src",
				"--v2=src/test/resources/module-library-v2/src", "--diff", "--plain", "--config=" + config))
				.succeeded()
				.stdoutContains("internal.Internal.m() EXECUTABLE_REMOVED");
		}

		@Test
		void yaml_fail_on_unresolved_is_honored(@TempDir Path tempDir) throws IOException {
			var config = write(tempDir, """
				diff:
				  failOnUnresolved: true
				""");
			var library = "src/test/resources/classpath-library/classpath-library.jar";
			var result = runExpectingDiagnostics("--v1=" + library, "--v2=" + library, "--diff", "--plain",
				"--config=" + config);

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.stderrContains("dependency.Base");
		}

		private static Path write(Path tempDir, String contents) throws IOException {
			Path config = tempDir.resolve("roseau.yaml");
			Files.writeString(config, contents);
			return config;
		}
	}

	@Nested
	class Verbosity {
		@Test
		void the_default_run_says_nothing_beyond_the_report() {
			assertThatCli(run(V1_SOURCES, V2_SOURCES, "--diff", "--plain"))
				.succeeded()
				.hasReport(FULL_DIFF)
				.hasNoStderr()
				.loggedNoWarnings();
		}

		@Test
		void verbose_traces_progress_on_stderr_and_leaves_stdout_alone() {
			var result = runExpectingDiagnostics(V1_SOURCES, V2_SOURCES, "--diff", "--plain", "-v");

			assertThatCli(result)
				.succeeded()
				.hasReport(FULL_DIFF);
			assertThat(CliResult.stabilize(result.stderr())).isEqualTo("""
				0 classpath entries for src/test/resources/test-project-v1/src (<n> ms)
				0 classpath entries for src/test/resources/test-project-v2/src (<n> ms)
				Building APIs...  1 types → 1 types (<n> ms)
				Comparing APIs... 4 breaking changes (<n> ms)
				""");
		}

		@Test
		void doubling_the_flag_adds_the_resolved_options_and_libraries() {
			var result = runExpectingDiagnostics(V1_SOURCES, V2_SOURCES, "--diff", "--plain", "-vv");

			assertThatCli(result)
				.succeeded()
				.hasReport(FULL_DIFF);
			assertThat(CliResult.stabilize(result.stderr()).lines().toList())
				.satisfiesExactly(
					options -> assertThat(options).startsWith("Options are RoseauOptions["),
					v1 -> assertThat(v1).startsWith("v1 = Library[location=src/test/resources/test-project-v1/src"),
					v2 -> assertThat(v2).startsWith("v2 = Library[location=src/test/resources/test-project-v2/src"),
					cp1 -> assertThat(cp1).isEqualTo(
						"0 classpath entries for src/test/resources/test-project-v1/src (<n> ms)"),
					cp2 -> assertThat(cp2).isEqualTo(
						"0 classpath entries for src/test/resources/test-project-v2/src (<n> ms)"),
					building -> assertThat(building).isEqualTo("Building APIs...  1 types → 1 types (<n> ms)"),
					comparing -> assertThat(comparing).isEqualTo("Comparing APIs... 4 breaking changes (<n> ms)"));
		}

		@Test
		void doubling_the_flag_turns_on_debug_logging() {
			var result = runExpectingDiagnostics(V1_SOURCES, V2_SOURCES, "--diff", "--plain", "-vv");

			assertThat(result.describeLogs())
				.contains("[DEBUG]")
				.contains("Diffing APIs took");
		}

		@Test
		void the_default_run_logs_nothing_at_all() {
			assertThat(run(V1_SOURCES, V2_SOURCES, "--diff", "--plain").logs()).isEmpty();
		}

		@Test
		void verbose_mode_prints_the_stack_trace_of_a_failure() {
			var result = runExpectingDiagnostics("--v1=src/test/resources/invalid-path", "--api", "--verbose");

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.reportedError("io.github.alien.roseau.RoseauException: Cannot find v1: src/test/resources/invalid-path")
				.stderrContains("\tat io.github.alien.roseau.cli.RoseauCLI");
		}

		@Test
		void the_default_run_points_at_the_verbose_flags_instead_of_a_stack_trace() {
			var result = runExpectingDiagnostics("--v1=src/test/resources/invalid-path", "--api");

			assertThatCli(result)
				.failed()
				.hasStderr("Cannot find v1: src/test/resources/invalid-path" + System.lineSeparator()
					+ "Use -v/-vv for detailed error logs." + System.lineSeparator());
		}
	}

	@Nested
	class Diagnostics {
		@Test
		void an_empty_library_warns_rather_than_failing() {
			CliResult result = runExpectingDiagnostics("--v1=src/test/resources/no-types",
				"--v2=src/test/resources/no-types", "--diff", "--plain");

			assertThatCli(result)
				.succeeded()
				.hasReport(NO_CHANGES)
				.hasNoStderr()
				.loggedWarning("No type found in");
		}

		@Test
		void a_library_without_exported_types_warns_rather_than_failing() {
			var result = runExpectingDiagnostics("--v1=src/test/resources/no-exported-types",
				"--v2=src/test/resources/no-exported-types", "--diff", "--plain");

			assertThatCli(result)
				.succeeded()
				.hasReport(NO_CHANGES)
				.loggedWarning("are exported: the API is empty");
		}

		@Test
		void warnings_never_reach_stdout() {
			var result = runExpectingDiagnostics("--v1=src/test/resources/no-types", "--api");

			assertThatCli(result)
				.succeeded()
				.stdoutAsJson();
			assertThat(result.warnings()).isNotEmpty();
		}
	}

	@Nested
	class MavenCoordinates {
		@Test
		@Timeout(value = 30, unit = TimeUnit.SECONDS)
		void api_mode_accepts_coordinates() {
			var result = runExpectingDiagnostics("--v1=com.aoapps:ao-lang:5.8.0", "--api");

			assertThatCli(result)
				.succeeded()
				.stderrDoesNotContain("Failed to download");
			assertThat(assertThatCli(result).stdoutAsJson().keySet()).contains("allTypes");
		}

		@Test
		@Timeout(value = 30, unit = TimeUnit.SECONDS)
		void diff_mode_accepts_coordinates() {
			var result = runExpectingDiagnostics("--v1=com.aoapps:ao-lang:5.0.0",
				"--v2=com.aoapps:ao-lang:5.8.0", "--diff", "--plain");

			assertThatCli(result)
				.succeeded()
				.stdoutContains("METHOD_NOW_FINAL")
				.stderrDoesNotContain("Failed to download");
		}

		@Test
		@Timeout(value = 30, unit = TimeUnit.SECONDS)
		void a_local_jar_can_be_compared_against_coordinates() {
			var result = runExpectingDiagnostics(V1_JAR, "--v2=com.aoapps:ao-lang:5.8.0",
				"--diff", "--plain");

			assertThatCli(result)
				.succeeded()
				.stdoutContains("TYPE_REMOVED")
				.stderrDoesNotContain("Failed to download");
		}

		@Test
		@Timeout(value = 30, unit = TimeUnit.SECONDS)
		void unknown_coordinates_name_what_could_not_be_downloaded() {
			var result = runExpectingDiagnostics("--v1=no-group:no-artifact:0.0.1", "--api");

			assertThatCli(result)
				.failed()
				.hasNoStdout()
				.reportedError("Failed to download no-group:no-artifact:0.0.1");
		}
	}
}
