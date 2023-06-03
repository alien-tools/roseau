package com.github.maracas.roseau;

import com.github.maracas.roseau.model.API;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.MavenLauncher;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

class APIExtractorTest {
	APIExtractor extractor;

	@BeforeEach
	void setUp() {
		Path sources = Path.of("src/test/resources/api-extractor-tests/without-modules/v1");
		Launcher launcher = new MavenLauncher(sources.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
		launcher.getEnvironment().setNoClasspath(true);
		extractor = new APIExtractor(launcher.buildModel());
	}

	@Test
	void write_some_interesting_tests_later() {
		API extracted = extractor.getAPI();
		assertThat(extracted.typeDeclarations(), hasSize(60));
	}
}