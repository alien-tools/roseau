package io.github.alien.roseau;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves raw library inputs into immutable {@link Library} instances.
 */
public final class LibraryResolver {
	private final MavenClasspathBuilder mavenClasspathBuilder;

	public LibraryResolver() {
		this(new MavenClasspathBuilder());
	}

	LibraryResolver(MavenClasspathBuilder mavenClasspathBuilder) {
		this.mavenClasspathBuilder = mavenClasspathBuilder;
	}

	public Library resolve(Path location) {
		return resolve(location, List.of(), null);
	}

	public Library resolve(Path location, List<Path> classpath) {
		return resolve(location, classpath, null);
	}

	public Library resolve(Path location, List<Path> classpath, Path pom) {
		List<Path> resolvedClasspath = new ArrayList<>(classpath == null ? List.of() : classpath);
		if (pom != null) {
			validatePom(pom);
			resolvedClasspath.addAll(mavenClasspathBuilder.buildClasspath(pom));
		}
		return Library.of(location, resolvedClasspath);
	}

	private static void validatePom(Path pom) {
		if (!Files.isRegularFile(pom) || !pom.toString().endsWith(".xml")) {
			throw new RoseauException("Invalid path to POM file: " + pom);
		}
	}
}
