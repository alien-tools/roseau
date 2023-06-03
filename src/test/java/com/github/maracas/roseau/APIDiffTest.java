package com.github.maracas.roseau;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.MavenLauncher;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

class APIDiffTest {
	APIDiff diff;

	@BeforeEach
	void setUp() {
		Path v1 = Path.of("src/test/resources/api-extractor-tests/without-modules/v1");
		Path v2 = Path.of("src/test/resources/api-extractor-tests/without-modules/v2");

		Launcher launcherV1 = new MavenLauncher(v1.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
		launcherV1.getEnvironment().setNoClasspath(true);
		Launcher launcherV2 = new MavenLauncher(v2.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
		launcherV2.getEnvironment().setNoClasspath(true);

		APIExtractor extractorV1 = new APIExtractor(launcherV1.buildModel());
		APIExtractor extractorV2 = new APIExtractor(launcherV2.buildModel());

		this.diff = new APIDiff(extractorV1.getAPI(), extractorV2.getAPI());
	}

	@Test
	void should_work() {
		diff.diffAPIs();
		assertThat(diff.getBreakingChanges(), hasSize(1));
	}
}