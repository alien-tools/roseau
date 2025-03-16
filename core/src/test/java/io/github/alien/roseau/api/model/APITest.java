package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.visit.AbstractAPIVisitor;
import io.github.alien.roseau.api.visit.Visit;
import io.github.alien.roseau.extractors.MavenClasspathBuilder;
import io.github.alien.roseau.extractors.jdt.JdtAPIExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class APITest {
	@Test
	void json_round_trip() throws IOException {
		Path sources = Path.of("src/main/java");
		MavenClasspathBuilder builder = new MavenClasspathBuilder();
		List<Path> classpath = builder.buildClasspath(Path.of("."));
		JdtAPIExtractor extractor = new JdtAPIExtractor();
		API orig = extractor.extractAPI(sources, classpath);

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
