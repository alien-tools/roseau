package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.CtModel;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class APITest {
	@Test
	void api_json_round_trip() throws IOException {
		Launcher l = new Launcher();
		l.addInputResource("/home/dig/repositories/maracas/forges/src/main/java/com/github/maracas/");
		l.getEnvironment().setIgnoreDuplicateDeclarations(true);
		l.getEnvironment().setComplianceLevel(17);
		CtModel m = l.buildModel();
		APIExtractor extractor = new SpoonAPIExtractor(m);
		API orig = extractor.extractAPI();

		Path p = Path.of("roundtrip.json");
		orig.writeJson(p);
		API res = API.fromJson(p);

		assertThat(res, is(equalTo(orig)));
	}
}
