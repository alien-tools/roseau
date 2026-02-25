package io.github.alien.roseau.maven;

import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenProjectSources;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

@MavenJupiterExtension
@MavenGoal("roseau:check")
@MavenProjectSources(sources = "simple-module-test")
class RoseauPluginGoalExecutionIT {
	@MavenTest
	void check_goal_without_packaging_reports_missing_artifact(MavenExecutionResult result) {
		assertThat(result).isFailure()
			.out().error().anyMatch(m -> m.contains("Current artifact not found."));
	}
}
