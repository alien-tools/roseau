package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.api.visit.Visit;
import com.github.maracas.roseau.extractors.sources.SpoonAPIExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class APITest {
	@Test
	void json_round_trip() throws IOException {
		Path sources = Path.of("src/test/resources/api-showcase");
		SpoonAPIExtractor extractor = new SpoonAPIExtractor();
		API orig = extractor.extractAPI(sources);

		Path json = Path.of("roundtrip.json");
		orig.writeJson(json);

		API res = API.fromJson(json, orig.getFactory());
		Files.delete(json);

		assertThat(res, is(equalTo(orig)));

		new AbstractAPIVisitor() {
			@Override
			public <T extends TypeDecl> Visit typeReference(TypeReference<T> ref) {
				return () -> {
					assertThat(ref + " cannot be resolved", ref.getResolvedApiType().isPresent(), is(true));
				};
			}
		}.$(res).visit();
	}
}
