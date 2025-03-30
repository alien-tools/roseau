package io.github.alien.roseau;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Library {
	private final Path path;
	private final List<Path> classpath;
	private final Path pom;

	private Library(Path path, List<Path> classpath, Path pom) {
		this.path = path;
		this.classpath = List.copyOf(classpath);
		this.pom = pom;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Library of(Path path) {
		return new Library(path, List.of(), null);
	}

	public Path getPath() {
		return path;
	}

	public List<Path> getClasspath() {
		return classpath;
	}

	public Path getPom() {
		return pom;
	}

	public boolean isJar() {
		return isJar(this.path);
	}

	public boolean isSources() {
		return isSources(this.path);
	}

	private static boolean isJar(Path file) {
		return file != null && Files.exists(file) && Files.isRegularFile(file) && file.toString().endsWith(".jar");
	}

	private static boolean isSources(Path file) {
		return file != null && Files.exists(file) && Files.isDirectory(file);
	}

	private static boolean isValidSource(Path file) {
		return isJar(file) || isSources(file);
	}

	private static boolean isValidPom(Path pom) {
		return Files.exists(pom) && Files.isRegularFile(pom) && pom.toString().endsWith(".xml");
	}

	public static final class Builder {
		private Path path;
		private List<Path> classpath = List.of();
		private Path pom;

		private Builder() {

		}

		public Builder path(Path path) {
			this.path = path;
			return this;
		}

		public Builder classpath(List<Path> classpath) {
			this.classpath = classpath;
			return this;
		}

		public Builder pom(Path pom) {
			this.pom = pom;
			return this;
		}

		public Library build() {
			if (!isValidSource(path)) {
				throw new IllegalArgumentException("Invalid path to library; directory or JAR expected: " + path);
			}

			if (pom != null && !isValidPom(pom)) {
				throw new IllegalArgumentException("Invalid path to POM file: " + path);
			}

			return new Library(path, classpath, pom);
		}
	}
}
