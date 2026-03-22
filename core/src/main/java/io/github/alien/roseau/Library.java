package io.github.alien.roseau;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * A library, in source or compiled form, provided for analysis.
 * <p>
 * The granularity of a library is that of a module, i.e., it should contain at most one module declaration
 * ({@code module-info.java}). If no module declaration is present, it is assumed that all names within this library
 * are implicitly exported. If a module declaration is present, the API accounts for unqualified {@code exports}
 * directives.
 *
 * @param location  A physical location that is either:
 *                  <ul>
 *                    <li>A source directory containing source files and one module declaration at most</li>
 *                    <li>A {@code module-info.java}; the directory containing the module is used as root directory</li>
 *                    <li>A JAR file containing at most one {@code module-info.java} file</li>
 *                  </ul>
 * @param classpath A resolved list of JAR files to be used as classpath entries
 */
public record Library(Path location, List<Path> classpath) {
	public Library {
		location = normalizeLocation(Preconditions.checkNotNull(location));
		classpath = normalizeClasspath(List.copyOf(Preconditions.checkNotNull(classpath)));
	}

	public static Library of(Path location) {
		return new Library(location, List.of());
	}

	public static Library of(Path location, List<Path> classpath) {
		return new Library(location, classpath);
	}

	public boolean isJar() {
		return isJar(location);
	}

	public boolean isSources() {
		return isSources(location);
	}

	private static Path normalizeLocation(Path location) {
		if (!isSources(location) && !isJar(location) && !isModuleInfo(location)) {
			throw new RoseauException("Invalid path to library; directory or JAR expected: " + location);
		}

		Path normalized = isModuleInfo(location)
			? location.toAbsolutePath().normalize().getParent()
			: location.toAbsolutePath().normalize();

		if (isSources(normalized) && hasMultipleModuleInfo(normalized)) {
			throw new RoseauException("A library cannot contain multiple module-info.java");
		}

		return normalized;
	}

	private static List<Path> normalizeClasspath(List<Path> classpath) {
		if (classpath == null) {
			return List.of();
		}

		return classpath.stream()
			.filter(Objects::nonNull)
			.map(Path::toAbsolutePath)
			.map(Path::normalize)
			.distinct()
			.toList();
	}

	private static boolean isJar(Path location) {
		if (location == null || !Files.isRegularFile(location)) {
			return false;
		}

		try (var _ = new ZipFile(location.toFile())) {
			return true;
		} catch (IOException _) {
			return false;
		}
	}

	private static boolean isSources(Path location) {
		return location != null && Files.isDirectory(location);
	}

	private static boolean isModuleInfo(Path file) {
		return file != null && "module-info.java".equals(file.getFileName().toString()) && Files.isRegularFile(file);
	}

	private static boolean hasMultipleModuleInfo(Path location) {
		try (Stream<Path> s = Files.find(
			location,
			Integer.MAX_VALUE,
			(Path p, BasicFileAttributes a) -> isModuleInfo(p)
		)) {
			return s.limit(2L).count() > 1L;
		} catch (IOException e) {
			return false;
		}
	}
}
