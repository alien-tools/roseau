package io.github.alien.roseau;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasspathResolverTest {
	@Test
	void resolve_merges_custom_and_pom(@TempDir Path tempDir) {
		var cp = tempDir.resolve("cp.jar");
		var pom = Path.of("pom.xml");
		var classpath = new ClasspathResolver().resolve(List.of(cp), pom);
		assertThat(classpath).contains(cp.toAbsolutePath().normalize()).hasSizeGreaterThan(1);
	}

	@Test
	void resolve_normalizes_and_deduplicates_merged_classpath(@TempDir Path tempDir) throws IOException {
		var pom = tempDir.resolve("pom.xml");
		var cp = Files.createFile(tempDir.resolve("cp.jar"));
		Files.createFile(pom);

		var resolver = new ClasspathResolver(new MavenClasspathBuilder() {
			@Override
			public List<Path> buildClasspath(Path ignored) {
				return List.of(cp, tempDir.resolve("nested").resolve("..").resolve("cp.jar"));
			}
		});

		var classpath = resolver.resolve(List.of(tempDir.resolve(".").resolve("cp.jar")), pom);

		assertThat(classpath).containsExactly(cp.toAbsolutePath().normalize());
	}
}
