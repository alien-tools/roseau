package io.github.alien.roseau;

import io.github.alien.roseau.extractors.ExtractorType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * A library, in source or compiled form, provided for analysis. The library points to a physical location and can be
 * complemented with a custom classpath or {@code pom.xml} file for dependency resolution. The extractor used to parse
 * and infer types can be customized. Use {@link #of(Path)} or the associated {@link Builder} to create new instances.
 */
public final class Library {
	private final Path location;
	private final List<Path> classpath;
	private final Path pom;
	private final ExtractorType extractorType;

	private Library(Path location, List<Path> classpath, Path pom, ExtractorType extractorType) {
		this.location = location.toAbsolutePath();
		this.classpath = List.copyOf(classpath);
		this.pom = pom;
		this.extractorType = extractorType;
	}

	/**
	 * Returns a new {@link Builder} for constructing library instances.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Constructs a new library instance from the given physical location.
	 *
	 * @param location the physical location of the library's sources or JAR
	 * @return a new library instance
	 */
	public static Library of(Path location) {
		return new Builder().location(location).build();
	}

	public Path getLocation() {
		return location;
	}

	public List<Path> getClasspath() {
		return classpath;
	}

	public Path getPom() {
		return pom;
	}

	public ExtractorType getExtractorType() {
		return extractorType;
	}

	public boolean isJar() {
		return isJar(this.location);
	}

	public boolean isSources() {
		return isSources(this.location);
	}

	private static boolean isJar(Path file) {
		// Can't do much more, except listing all extensions (jar, war, ear, jmod, etc.)
		return file != null && Files.exists(file) && Files.isRegularFile(file);
	}

	private static boolean isSources(Path file) {
		return file != null && Files.exists(file) && Files.isDirectory(file);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Library library = (Library) o;
		return Objects.equals(location, library.location) &&
			Objects.equals(classpath, library.classpath) &&
			Objects.equals(pom, library.pom) &&
			extractorType == library.extractorType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(location, classpath, pom, extractorType);
	}

	/**
	 * Builder class for constructing {@link Library} instances. Use the provided methods to set the physical location,
	 * classpath, {@code pom.xml} file, and {@link ExtractorType} of the library, and invoke {@link #build()} to create
	 * the corresponding {@link Library}. Only the library's location is required.
	 */
	public static final class Builder {
		private Path location;
		private List<Path> classpath = List.of();
		private Path pom;
		private ExtractorType extractorType;

		private Builder() {

		}

		/**
		 * Sets the library's physical location, either a JAR file or sources directory.
		 *
		 * @param location the physical location
		 * @return this builder
		 */
		public Builder location(Path location) {
			this.location = location;
			return this;
		}

		/**
		 * Sets the library's classpath.
		 *
		 * @param classpath the classpath
		 * @return this builder
		 */
		public Builder classpath(List<Path> classpath) {
			this.classpath = classpath;
			return this;
		}

		/**
		 * Sets the library's {@code pom.xml} file to be used for dependency resolution.
		 *
		 * @param pom the {@code pom.xml} file
		 * @return this builder
		 */
		public Builder pom(Path pom) {
			this.pom = pom;
			return this;
		}

		/**
		 * Sets the extractor to be used for parsing and inferring types.
		 *
		 * @param extractorType the desired {@link ExtractorType}
		 * @return this builder
		 */
		public Builder extractorType(ExtractorType extractorType) {
			this.extractorType = extractorType;
			return this;
		}

		private static boolean isValidSource(Path location) {
			return isJar(location) || isSources(location);
		}

		private static boolean isValidPom(Path pom) {
			return Files.exists(pom) && Files.isRegularFile(pom) && pom.toString().endsWith(".xml");
		}

		/**
		 * Constructs and returns a new {@link Library} instance based on the parameters set in the builder.
		 *
		 * @return the new instance
		 * @throws IllegalArgumentException if the library's location is invalid, if the POM file path is invalid, or if the
		 *                                  specified extractor type is incompatible with the library's type
		 */
		public Library build() {
			if (!isValidSource(location)) {
				throw new IllegalArgumentException("Invalid path to library; directory or JAR expected: " + location);
			}

			if (pom != null && !isValidPom(pom)) {
				throw new IllegalArgumentException("Invalid path to POM file: " + pom);
			}

			if (extractorType == null) {
				if (isJar(location)) {
					extractorType = ExtractorType.ASM;
				} else {
					extractorType = ExtractorType.JDT;
				}
			}

			if (extractorType == ExtractorType.ASM && isSources(location)) {
				throw new IllegalArgumentException("ASM extractor cannot be used on source directories");
			}

			if ((extractorType == ExtractorType.SPOON || extractorType == ExtractorType.JDT) && isJar(location)) {
				throw new IllegalArgumentException("Source extractors cannot be used on JARs");
			}

			return new Library(location, classpath, pom, extractorType);
		}
	}
}
