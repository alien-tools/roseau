package io.github.alien.roseau.api.model;

import io.github.alien.roseau.ExclusionOptions;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.visit.AbstractAPIVisitor;
import io.github.alien.roseau.api.visit.Visit;
import io.github.alien.roseau.extractors.MavenClasspathBuilder;
import io.github.alien.roseau.extractors.jdt.JdtAPIExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.alien.roseau.utils.TestUtils.buildJdtAPI;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

class APITest {
	@Test
	void json_round_trip() throws IOException {
		var sources = Path.of("src/main/java");
		var builder = new MavenClasspathBuilder();
		var classpath = builder.buildClasspath(Path.of("."));
		var extractor = new JdtAPIExtractor();
		var orig = extractor.extractAPI(sources, classpath);

		var json = Path.of("roundtrip.json");
		orig.writeJson(json);

		var res = API.fromJson(json, orig.getFactory());
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

	@Test
	void name_based_exclusion() {
		var api = buildJdtAPI("public class AClass {}");

		var excludeClass = ExclusionOptions.builder().excludeSymbol("AClass").build();

		assertThat(api.getExportedTypes().toList(), hasSize(1));
		assertThat(api.getExportedTypes(excludeClass).toList(), hasSize(0));
	}

	@Test
	void annotation_based_exclusion() {
		var api = buildJdtAPI("@Deprecated public class AClass {}");

		var excludeDeprecated = ExclusionOptions.builder()
			.excludeAnnotation("java.lang.Deprecated").build();

		assertThat(api.getExportedTypes().toList(), hasSize(1));
		assertThat(api.getExportedTypes(excludeDeprecated).toList(), hasSize(0));
	}
}
