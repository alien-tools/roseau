package io.github.alien.roseau.maven;

import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenProjectSources;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.extension.SystemProperty;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;
import org.junit.jupiter.api.Nested;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

@MavenJupiterExtension
@MavenGoal("verify")
class RoseauPluginIT {
	@Nested
	@MavenProjectSources(sources = "simple-module-test")
	class NestedSetup {
		@MavenTest
		void bcs_are_reported(MavenExecutionResult result) {
			assertThat(result).isSuccessful()
				.out().warn().anyMatch(m -> m.contains("METHOD_REMOVED"))
				.anyMatch(m -> m.contains("TYPE_NEW_ABSTRACT_METHOD"));
		}

		@SystemProperty("roseau.failOnBinaryIncompatibility")
		@MavenTest
		void bcs_can_fail_the_build(MavenExecutionResult result) {
			assertThat(result).isFailure()
				.out().error().anyMatch(m -> m.contains("Binary incompatible changes found"));
		}

		@SystemProperty("roseau.skip")
		@MavenTest
		void roseau_can_be_skipped(MavenExecutionResult result) {
			assertThat(result).isSuccessful();
			assertThat(result).out().error().isEmpty();
			assertThat(result).out().warn().noneMatch(m -> m.contains("METHOD_ADDED_TO_INTERFACE"));
			assertThat(result).out().info().anyMatch(m -> m.contains("Skipping."));
		}
	}

	@Nested
	@MavenProjectSources(sources = "report-formats-test")
	class ReportFormatsTest {
		@MavenTest
		void reports_are_generated(MavenExecutionResult result) {
			assertThat(result).isSuccessful();

			Path targetDir = result.getMavenProjectResult().getTargetProjectDirectory();
			Path roseauDir = targetDir.resolve("target/roseau");

			// Check that all report files are created
			Path htmlReport = roseauDir.resolve("roseau-report.html");
			Path csvReport = roseauDir.resolve("roseau-report.csv");
			Path jsonReport = roseauDir.resolve("roseau-report.json");

			assertThat(Files.exists(htmlReport))
				.as("HTML report should exist at " + htmlReport)
				.isTrue();
			assertThat(Files.exists(csvReport))
				.as("CSV report should exist at " + csvReport)
				.isTrue();
			assertThat(Files.exists(jsonReport))
				.as("JSON report should exist at " + jsonReport)
				.isTrue();

			// Verify log messages
			assertThat(result).out().info()
				.anyMatch(m -> m.contains("Report written to"));
		}
	}

	@Nested
	@MavenProjectSources(sources = "classpath-auto-inclusion-test")
	class ClasspathAutoInclusionTest {
		@MavenTest
		void dependencies_are_auto_included(MavenExecutionResult result) {
			// The build should succeed because dependencies are on the classpath
			assertThat(result).isSuccessful();
		}
	}

	@Nested
	@MavenProjectSources(sources = "exclusion-name-test")
	class ExclusionNameTest {
		@MavenTest
		void internal_packages_are_excluded(MavenExecutionResult result) {
			assertThat(result).isSuccessful();

			// Should NOT report breaking changes in pkg.internal.*
			assertThat(result).out().warn()
				.noneMatch(m -> m.contains("pkg.internal"));

			// Should still report breaking changes in pkg.*
			assertThat(result).out().warn()
				.anyMatch(m -> m.contains("pkg.PublicAPI") || m.contains("TYPE_"));
		}
	}

	@Nested
	@MavenProjectSources(sources = "binary-only-test")
	class BinaryOnlyTest {
		@MavenTest
		void only_binary_breaking_changes_reported(MavenExecutionResult result) {
			assertThat(result).isSuccessful();

			// Should report binary breaking changes
			assertThat(result).out().warn()
				.anyMatch(m -> m.contains("METHOD_REMOVED") || m.contains("TYPE_"));
		}
	}

	@Nested
	@MavenProjectSources(sources = "ignored-csv-test")
	class IgnoredCsvTest {
		@MavenTest
		void ignored_changes_not_reported(MavenExecutionResult result) {
			assertThat(result).isSuccessful();

			// METHOD_REMOVED should be ignored per CSV file
			// But TYPE_NEW_ABSTRACT_METHOD should still be reported
			assertThat(result).out().warn()
				.noneMatch(m -> m.contains("METHOD_REMOVED"));

			// Other breaking changes should still be reported
			assertThat(result).out().warn()
				.anyMatch(m -> m.contains("TYPE_NEW_ABSTRACT_METHOD"));
		}
	}

	@Nested
	@MavenProjectSources(sources = "api-export-test")
	class ApiExportTest {
		@MavenTest
		void api_files_are_exported(MavenExecutionResult result) {
			assertThat(result).isSuccessful();

			Path targetDir = result.getMavenProjectResult().getTargetProjectDirectory();

			// Check that API JSON files are created
			Path baselineApi = targetDir.resolve("target/baseline-api.json");
			Path currentApi = targetDir.resolve("target/current-api.json");

			assertThat(Files.exists(baselineApi))
				.as("Baseline API should be exported to " + baselineApi)
				.isTrue();
			assertThat(Files.exists(currentApi))
				.as("Current API should be exported to " + currentApi)
				.isTrue();

			// Verify log messages
			assertThat(result).out().info()
				.anyMatch(m -> m.contains("Baseline API exported"))
				.anyMatch(m -> m.contains("Current API exported"));
		}
	}

	@Nested
	@MavenProjectSources(sources = "multiple-reports-test")
	class MultipleReportsTest {
		@MavenTest
		void all_report_formats_generated(MavenExecutionResult result) {
			assertThat(result).isSuccessful();

			Path targetDir = result.getMavenProjectResult().getTargetProjectDirectory();
			Path roseauDir = targetDir.resolve("target/roseau");

			// Check that all report files are created
			assertThat(Files.exists(roseauDir.resolve("report.html")))
				.as("HTML report should exist")
				.isTrue();
			assertThat(Files.exists(roseauDir.resolve("report.csv")))
				.as("CSV report should exist")
				.isTrue();
			assertThat(Files.exists(roseauDir.resolve("report.json")))
				.as("JSON report should exist")
				.isTrue();
			assertThat(Files.exists(roseauDir.resolve("report.md")))
				.as("Markdown report should exist")
				.isTrue();

			// Verify that reports were written
			assertThat(result).out().info()
				.anyMatch(m -> m.contains("Report written to"));
		}
	}

	@Nested
	@MavenProjectSources(sources = "yaml-config-test")
	class YamlConfigTest {
		@MavenTest
		void yaml_configuration_is_loaded(MavenExecutionResult result) {
			assertThat(result).isSuccessful();

			Path targetDir = result.getMavenProjectResult().getTargetProjectDirectory();

			// Check that report from YAML config was created
			Path yamlReport = targetDir.resolve("target").resolve("roseau").resolve("yaml-report.html");
			assertThat(Files.exists(yamlReport))
				.as("Report from YAML config should exist at " + yamlReport)
				.isTrue();
		}
	}

	@Nested
	@MavenProjectSources(sources = "fail-on-binary-test")
	class FailOnBinaryTest {
		@MavenTest
		void build_fails_on_binary_incompatibility(MavenExecutionResult result) {
			// Build should fail due to binary incompatibility
			assertThat(result).isFailure();

			// Check error message
			assertThat(result).out().error()
				.anyMatch(m -> m.contains("Binary incompatible changes found"));
		}
	}
}
