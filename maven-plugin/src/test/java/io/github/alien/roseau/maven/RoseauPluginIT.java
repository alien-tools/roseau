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

import java.nio.file.Files;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

@MavenJupiterExtension
@MavenGoal("verify")
class RoseauPluginIT {
	@Nested
	@MavenProjectSources(sources = "simple-module-test")
	class SimpleModule {
		@MavenTest
		void bcs_are_reported(MavenExecutionResult result) {
			assertThat(result).isSuccessful()
				.out().warn()
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
				.noneMatch(m -> m.contains("pkg.I TYPE_NEW_ABSTRACT_METHOD"))
				.anyMatch(m -> m.contains("pkg.C.f FIELD_NOW_STATIC"));
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
		void roseau_can_be_skipped(MavenExecutionResult result) {
			assertThat(result).isSuccessful();
			assertThat(result).out().error().isEmpty();
			assertThat(result).out().warn()
				.noneMatch(m -> m.contains("pkg.I.foo() METHOD_REMOVED"))
				.noneMatch(m -> m.contains("pkg.I TYPE_NEW_ABSTRACT_METHOD"))
				.noneMatch(m -> m.contains("pkg.C.f FIELD_NOW_STATIC"));
			assertThat(result).out().info().anyMatch(m -> m.contains("Skipping."));
		}

		@MavenOption(MavenCLIOptions.VERBOSE)
		@MavenTest
		void only_compile_dependencies_are_included(MavenExecutionResult result) {
			assertThat(result).isSuccessful();
			assertThat(result).out().debug()
				.anyMatch(m -> m.matches("^v1 classpath is: \\[.*org/slf4j/slf4j-api/2.0.17/slf4j-api-2\\.0\\.17\\.jar\\]$"))
				.anyMatch(m -> m.matches("^v2 classpath is: \\[.*org/slf4j/slf4j-api/2.0.17/slf4j-api-2\\.0\\.17\\.jar\\]$"));
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
	}

	@Nested
	@MavenProjectSources(sources = "module-with-yaml")
	class ModuleWithYaml {
		@MavenTest
		void yaml_is_loaded(MavenExecutionResult result) {
			// pkg.C is excluded
			// pkg.I.foo() is ignored
			// common-cp.jar in common
			// v2-cp.jar in v2
			// report.csv & report.html expected
			var baseDir = result.getMavenProjectResult().getTargetProjectDirectory();
			assertThat(result).isSuccessful()
				.out().info()
					.anyMatch(m -> m.matches("^Loaded configuration from .*/roseau.yaml$"));
			assertThat(result).out().warn()
				.anyMatch(m -> m.contains("pkg.I TYPE_NEW_ABSTRACT_METHOD"))
				.noneMatch(m -> m.contains("pkg.C.f FIELD_NOW_STATIC"))
				.noneMatch(m -> m.contains("pkg.I.foo() METHOD_REMOVED"));
			assertThat(baseDir.resolve("report.csv")).exists();
			assertThat(baseDir.resolve("report.html")).exists();
		}
	}
}
