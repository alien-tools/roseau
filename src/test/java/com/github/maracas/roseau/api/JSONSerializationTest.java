package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.CtModel;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class JSONSerializationTest {
	@Test
	void api_json_round_trip() throws IOException {
		Path sources = Path.of("/home/dig/repositories/maracas/forges/src/main/java/com/github/maracas/");
		CtModel m = SpoonAPIExtractor.buildModel(sources, 10).orElseThrow();
		APIExtractor extractor = new SpoonAPIExtractor(m);
		API orig = extractor.extractAPI();

		Path p = Path.of("roundtrip.json");
		orig.writeJson(p);
		API res = API.fromJson(p);

		assertThat(res, is(equalTo(orig)));
	}
}
