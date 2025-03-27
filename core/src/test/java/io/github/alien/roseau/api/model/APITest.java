package io.github.alien.roseau.api.model;

import io.github.alien.roseau.extractors.MavenClasspathBuilder;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
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
		JdtTypesExtractor extractor = new JdtTypesExtractor();
		LibraryTypes orig = extractor.extractTypes(sources, classpath);

		Path json = Path.of("roundtrip.json");
		orig.writeJson(json);

		LibraryTypes res = LibraryTypes.fromJson(json);
		Files.delete(json);

		assertThat(res, is(equalTo(orig)));
	}
}
