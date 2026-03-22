package io.github.alien.roseau;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves a clean classpath from user-provided entries and an optional {@code pom.xml}.
 */
public final class ClasspathResolver {
	private final MavenClasspathBuilder builder;

	public ClasspathResolver() {
		this(new MavenClasspathBuilder());
	}

	ClasspathResolver(MavenClasspathBuilder builder) {
		this.builder = Objects.requireNonNull(builder);
	}

	public List<Path> resolve(List<Path> classpath, Path pom) {
		List<Path> resolvedClasspath = new ArrayList<>(classpath == null ? List.of() : classpath);
		if (pom != null) {
			validatePom(pom);
			resolvedClasspath.addAll(builder.buildClasspath(pom));
		}
		return resolvedClasspath.stream()
			.filter(Objects::nonNull)
			.map(Path::toAbsolutePath)
			.map(Path::normalize)
			.distinct()
			.toList();
	}

	private static void validatePom(Path pom) {
		if (!Files.isRegularFile(pom) || !pom.toString().endsWith(".xml")) {
			throw new RoseauException("Invalid path to POM file: " + pom);
		}
	}
}
