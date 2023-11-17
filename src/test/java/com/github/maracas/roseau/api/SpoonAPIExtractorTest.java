package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.CtModel;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpoonAPIExtractorTest {
	static SpoonAPIExtractor extractor;
	static CtModel model;

	@BeforeAll
	static void setUp() {
		Path sources = Path.of("/home/dig/repositories/maracas/core/src/main/java");
		Launcher launcher = new Launcher();
		launcher.addInputResource(sources.toAbsolutePath().toString());
		launcher.getEnvironment().setNoClasspath(true);
		launcher.getEnvironment().setComplianceLevel(17);
		model = launcher.buildModel();
		extractor = new SpoonAPIExtractor(model);
	}

	@Test
	void write_some_interesting_tests_later() throws IOException {
		// Extracting data and processing it
		API api = extractor.extractAPI();

		// Printing the API for each type
		//System.out.println(api.toString());

		api.writeJson(Path.of("api.json"));
	}
}
