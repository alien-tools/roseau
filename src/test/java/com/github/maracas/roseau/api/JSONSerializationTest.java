package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.api.visit.Visit;
import org.junit.jupiter.api.Test;
import spoon.reflect.CtModel;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JSONSerializationTest {
	@Test
	void api_json_round_trip() throws IOException {
		Path sources = Path.of("/home/dig/repositories/maracas/forges/src/main/java/com/github/maracas/");
		CtModel m = SpoonAPIExtractor.buildModel(sources, 10).orElseThrow();
		APIExtractor extractor = new SpoonAPIExtractor(m);
		API orig = extractor.extractAPI();

		Path p = Path.of("roundtrip.json");
		orig.writeJson(p);

		API res = API.fromJson(p, orig.getFactory());

		assertThat(res, is(equalTo(orig)));

		new AbstractAPIVisitor() {
			@Override
			public <T extends TypeDecl> Visit typeReference(TypeReference<T> ref) {
				return () -> {
					assertThat(ref.getFactory(), is(notNullValue()));
				};
			}
		}.$(res).visit();

		System.out.println(res);
	}
}
