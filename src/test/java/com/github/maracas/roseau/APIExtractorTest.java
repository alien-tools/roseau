package com.github.maracas.roseau;

import com.github.maracas.roseau.model.API;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import java.nio.file.Path;




class APIExtractorTest {
	APIExtractor extractor;
	CtModel model;


	@BeforeEach
	void setUp() {
		Path sources = Path.of("src/test/resources/api-extractor-tests/without-modules/v1");
		Launcher launcher = new MavenLauncher(sources.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
		launcher.getEnvironment().setNoClasspath(true);
		model = launcher.buildModel();
		extractor = new APIExtractor(model);

	}

	@Test
	void write_some_interesting_tests_later() {
		// Extracting data and processing it
		API api = extractor.dataProcessing(extractor);

		// Printing the API for each type
		//extractor.printingData(api);
		//extractor.trying();
	}


}