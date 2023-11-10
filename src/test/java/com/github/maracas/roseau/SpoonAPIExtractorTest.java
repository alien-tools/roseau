package com.github.maracas.roseau;

import com.github.maracas.roseau.model.API;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpoonAPIExtractorTest {
	static SpoonAPIExtractor extractor;
	static CtModel model;

	@BeforeAll
	static void setUp() {
		Path sources = Path.of("src/test/resources/api-extractor-tests/without-modules/v1");
		Launcher launcher = new MavenLauncher(sources.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
		launcher.getEnvironment().setNoClasspath(true);
		launcher.getEnvironment().setComplianceLevel(17);
		model = launcher.buildModel();
		extractor = new SpoonAPIExtractor(model);
	}

	@Test
	void write_some_interesting_tests_later() {
		// Extracting data and processing it
		API api = extractor.extractAPI();

		// Printing the API for each type
		System.out.println(api.toString());
	}
}
