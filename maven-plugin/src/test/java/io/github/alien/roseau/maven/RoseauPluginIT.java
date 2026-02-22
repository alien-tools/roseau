package io.github.alien.roseau.maven;

import com.soebes.itf.jupiter.extension.MavenCLIOptions;
import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenOption;
import com.soebes.itf.jupiter.extension.MavenProjectSources;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.extension.SystemProperty;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;
import org.junit.jupiter.api.Nested;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

@MavenJupiterExtension
@MavenGoal("verify")
class RoseauPluginIT {
	@Nested
	@MavenProjectSources(sources = "simple-module-test")
	class SimpleModule {
		@MavenTest
		void bcs_are_reported(MavenExecutionResult result) {
			assertThat(result).isSuccessful();
			assertThat(result).out().error().isEmpty();
			assertThat(result).out().warn()
				.anyMatch(m -> m.contains("pkg.I.foo() METHOD_REMOVED"))
				.anyMatch(m -> m.contains("pkg.I TYPE_NEW_ABSTRACT_METHOD"))
				.anyMatch(m -> m.contains("pkg.C.f FIELD_NOW_STATIC"));
		}

		@SystemProperty("roseau.binaryOnly")
		@MavenTest
		void can_filter_binary_changes(MavenExecutionResult result) {
			assertThat(result).isSuccessful()
				.out().warn()
				.anyMatch(m -> m.contains("pkg.I.foo() METHOD_REMOVED"))
				.anyMatch(m -> m.contains("pkg.C.f FIELD_NOW_STATIC"))
				.noneMatch(m -> m.contains("pkg.I TYPE_NEW_ABSTRACT_METHOD"));
		}

		@SystemProperty("roseau.sourceOnly")
		@MavenTest
		void can_filter_source_changes(MavenExecutionResult result) {
			assertThat(result).isSuccessful()
				.out().warn()
				.anyMatch(m -> m.contains("pkg.I.foo() METHOD_REMOVED"))
				.anyMatch(m -> m.contains("pkg.I TYPE_NEW_ABSTRACT_METHOD"))
				.noneMatch(m -> m.contains("pkg.C.f FIELD_NOW_STATIC"));
		}

		@SystemProperty("roseau.failOnIncompatibility")
		@MavenTest
		void bcs_can_fail_the_build(MavenExecutionResult result) {
			assertThat(result).isFailure()
				.out().error().anyMatch(m -> m.contains("Breaking changes found"));
		}

		@SystemProperty("roseau.failOnBinaryIncompatibility")
		@SystemProperty("roseau.binaryOnly")
		@MavenTest
		void binary_bcs_can_fail_the_build(MavenExecutionResult result) {
			assertThat(result).isFailure()
				.out().error().anyMatch(m -> m.contains("Binary incompatible changes found"));
		}

		@SystemProperty("roseau.failOnSourceIncompatibility")
		@SystemProperty("roseau.sourceOnly")
		@MavenTest
		void source_bcs_can_fail_the_build(MavenExecutionResult result) {
			assertThat(result).isFailure()
				.out().error().anyMatch(m -> m.contains("Source incompatible changes found"));
		}

		@SystemProperty("roseau.skip")
		@MavenTest
		void can_be_skipped(MavenExecutionResult result) {
			assertThat(result).isSuccessful();
			assertThat(result).out().error().isEmpty();
			assertThat(result).out().warn()
				.noneMatch(m -> m.contains("pkg.I.foo() METHOD_REMOVED"))
				.noneMatch(m -> m.contains("pkg.I TYPE_NEW_ABSTRACT_METHOD"))
				.noneMatch(m -> m.contains("pkg.C.f FIELD_NOW_STATIC"));
			assertThat(result).out().info().anyMatch(m -> m.contains("Skipping."));
		}

		@SystemProperty(value = "roseau.baselineJar", content = "missing.jar")
		@MavenTest
		void baseline_jar_from_pom_configuration_takes_precedence_over_system_property(MavenExecutionResult result) {
			assertThat(result).isSuccessful()
				.out().warn()
				.anyMatch(m -> m.contains("pkg.I.foo() METHOD_REMOVED"))
				.anyMatch(m -> m.contains("pkg.I TYPE_NEW_ABSTRACT_METHOD"))
				.anyMatch(m -> m.contains("pkg.C.f FIELD_NOW_STATIC"));
			assertThat(result).out().error().noneMatch(m -> m.contains("Invalid baseline JAR"));
		}

		@SystemProperty(value = "roseau.configFile", content = "missing.yaml")
		@MavenTest
		void invalid_config_file_fails(MavenExecutionResult result) {
			assertThat(result).isFailure()
				.out().error().anyMatch(m -> m.contains("Could not load configuration file"));
		}

		@SystemProperty(value = "roseau.exportBaselineApi", content = "target/apis/v1.json")
		@SystemProperty(value = "roseau.exportCurrentApi", content = "target/apis/v2.json")
		@MavenTest
		void apis_can_be_exported(MavenExecutionResult result) {
			var baseDir = result.getMavenProjectResult().getTargetProjectDirectory();
			assertThat(result).isSuccessful();
			assertThat(baseDir.resolve("target/apis/v1.json")).isRegularFile().isNotEmptyFile();
			assertThat(baseDir.resolve("target/apis/v2.json")).isRegularFile().isNotEmptyFile();
		}

		@MavenOption(MavenCLIOptions.VERBOSE)
		@MavenTest
		void only_compile_dependencies_are_included(MavenExecutionResult result) {
			assertThat(result).isSuccessful();
			assertThat(result).out().debug()
				.anyMatch(m -> m.contains("v1 classpath is:") && m.contains("org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar"))
				.anyMatch(m -> m.contains("v2 classpath is:") && m.contains("org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar"));
		}

		@SystemProperty(value = "roseau.configFile", content = "roseau-classpath-config.yaml")
		@MavenOption(MavenCLIOptions.VERBOSE)
		@MavenTest
		void classpath_configuration_is_applied_to_each_version(MavenExecutionResult result) {
			assertThat(result).isSuccessful();
			assertThat(result).out().debug()
				.anyMatch(m -> m.contains("v1 classpath is: ") && m.contains("manual-baseline.jar"));
		}

		@SystemProperty(value = "roseau.configFile", content = "roseau-yaml-config.yaml")
		@MavenTest
		void yaml_is_loaded(MavenExecutionResult result) {
			var baseDir = result.getMavenProjectResult().getTargetProjectDirectory();
			assertThat(result).isSuccessful()
				.out().info()
				.anyMatch(m -> m.contains("Loaded configuration from") && m.contains("roseau-yaml-config.yaml"));
			assertThat(result).out().warn()
				.anyMatch(m -> m.contains("pkg.I TYPE_NEW_ABSTRACT_METHOD"))
				.noneMatch(m -> m.contains("pkg.C.f FIELD_NOW_STATIC"))
				.noneMatch(m -> m.contains("pkg.I.foo() METHOD_REMOVED"));
			assertThat(baseDir.resolve("report.csv")).doesNotExist();
			assertThat(baseDir.resolve("report.html")).doesNotExist();
			assertThat(baseDir.resolve("target/roseau/report.csv")).exists();
			assertThat(baseDir.resolve("target/roseau/report.html")).exists();
		}

		@SystemProperty(value = "roseau.configFile", content = "roseau-yaml-config.yaml")
		@SystemProperty(value = "roseau.binaryOnly")
		@MavenTest
		void maven_properties_override_yaml_options(MavenExecutionResult result) {
			assertThat(result).isSuccessful();
			assertThat(result).out().warn().noneMatch(m -> m.contains("TYPE_NEW_ABSTRACT_METHOD"));
			assertThat(result).out().info().anyMatch(m -> m.contains("No breaking changes found."));
		}

		@SystemProperty(value = "roseau.configFile", content = "roseau-source-only.yaml")
		@MavenTest
		void yaml_properties_are_applied(MavenExecutionResult result) {
			assertThat(result).isSuccessful();
			assertThat(result).out().warn()
				.anyMatch(m -> m.contains("pkg.I.foo() METHOD_REMOVED"))
				.anyMatch(m -> m.contains("pkg.I TYPE_NEW_ABSTRACT_METHOD"))
				.noneMatch(m -> m.contains("pkg.C.f FIELD_NOW_STATIC"));
		}

		@SystemProperty(value = "roseau.configFile", content = "roseau-yaml-config.yaml")
		@SystemProperty(value = "roseau.reportDirectory", content = "target/foo")
		@MavenTest
		void report_directory_parameter_is_applied(MavenExecutionResult result) {
			var baseDir = result.getMavenProjectResult().getTargetProjectDirectory();
			assertThat(result).isSuccessful();
			assertThat(baseDir.resolve("target/roseau/report.csv")).doesNotExist();
			assertThat(baseDir.resolve("target/roseau/report.html")).doesNotExist();
			assertThat(baseDir.resolve("target/foo/report.csv")).isRegularFile().isNotEmptyFile();
			assertThat(baseDir.resolve("target/foo/report.html")).isRegularFile().isNotEmptyFile();
		}
	}

	@Nested
	@MavenProjectSources(sources = "module-with-maven-baseline")
	class ModuleWithMavenBaseline {
		@MavenTest
		void baseline_is_resolved(MavenExecutionResult result) {
			assertThat(result).isSuccessful()
				.out().warn()
				.anyMatch(m -> m.contains("io.github.alien.roseau.Library TYPE_REMOVED"));
		}

		@SystemProperty(value = "roseau.baselineJar", content = "missing.jar")
		@MavenTest
		void baseline_version_takes_precedence_over_baseline_jar(MavenExecutionResult result) {
			assertThat(result).isSuccessful()
				.out().warn().anyMatch(m -> m.contains("io.github.alien.roseau.Library TYPE_REMOVED"));
			assertThat(result).out().error().noneMatch(m -> m.contains("Invalid baseline JAR"));
		}
	}

	@Nested
	@MavenProjectSources(sources = "no-baseline-test")
	class NoBaseline {
		@MavenTest
		void missing_baseline_is_reported_and_check_is_skipped(MavenExecutionResult result) {
			assertThat(result).isFailure()
				.out().error().anyMatch(m -> m.contains("No baseline specified"));
		}
	}

	@Nested
	@MavenProjectSources(sources = "invalid-maven-baseline-test")
	class InvalidMavenBaseline {
		@MavenTest
		void invalid_baseline_version_is_reported_and_check_is_skipped(MavenExecutionResult result) {
			assertThat(result).isFailure()
				.out().error().anyMatch(m -> m.contains("Couldn't resolve the baseline version"));
		}
	}

	@Nested
	@MavenProjectSources(sources = "pom-packaging-test")
	class PomPackaging {
		@MavenTest
		void pom_packaging_is_skipped(MavenExecutionResult result) {
			assertThat(result).isSuccessful()
				.out().info().anyMatch(m -> m.contains("Packaging of the project is 'pom'; skipping."));
		}
	}

	@Nested
	@MavenProjectSources(sources = "multi-module-paths-test")
	class MultiModulePaths {
		@MavenTest
		void module_relative_paths_are_resolved_against_each_module(MavenExecutionResult result) {
			var baseDir = result.getMavenProjectResult().getTargetProjectDirectory();
			assertThat(result).isSuccessful();
			assertThat(baseDir.resolve("module-a/target/roseau/report.csv")).isRegularFile().isNotEmptyFile();
			assertThat(baseDir.resolve("module-b/target/roseau/report.csv")).isRegularFile().isNotEmptyFile();
		}
	}

	@Nested
	@MavenProjectSources(sources = "maven-report-directory-test")
	class MavenReportDirectory {
		@MavenTest
		void report_directory_is_applied_to_reports_declared_in_maven_configuration(MavenExecutionResult result) {
			var baseDir = result.getMavenProjectResult().getTargetProjectDirectory();
			assertThat(result).isSuccessful();
			assertThat(baseDir.resolve("report.csv")).doesNotExist();
			assertThat(baseDir.resolve("report.html")).doesNotExist();
			assertThat(baseDir.resolve("target/foo/report.csv")).isRegularFile().isNotEmptyFile();
			assertThat(baseDir.resolve("target/foo/report.html")).isRegularFile().isNotEmptyFile();
		}
	}
}
