package io.github.alien.roseau;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Suppliers;
import io.github.alien.roseau.extractors.ExtractorType;
import io.github.alien.roseau.extractors.MavenClasspathBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * A library, in source or compiled form, provided for analysis.
 * <p>
 * The granularity of a library is that of a module, i.e., it should contain at most one module declaration
 * ({@code module-info.java}). If no module declaration is present, it is assumed that all names within this library
 * are implicitly exported. If a module declaration is present, the API accounts for unqualified {@code exports}
 * directives. The library points to a physical location that is either:
 * <ul>
 *   <li>A source directory containing nested names and source files and one module declaration at most</li>
 *   <li>A {@code module-info.java}. In this case, the directory containing the module is used as root directory</li>
 *   <li>A JAR file containing at most one {@code module-info.java} file</li>
 * </ul>
 * A library can be complemented with a custom classpath or a {@code pom.xml} file for dependency resolution. The
 * extractor used to parse and infer types can be customized. Use {@link #of(Path)} or {@link #builder()} to create new
 * instances.
 */
public final class Library {
	private final Path location;
	private final Set<Path> customClasspath;
	private final Path pom;
	private final ExtractorType extractorType;
	private final RoseauOptions.Exclude exclusions;
	@JsonIgnore
	private final Supplier<Set<Path>> classpath;

	/**
	 * Use the provided {@link #of(Path)} or {@link #builder()} instead.
	 */
	private Library(Path location, Set<Path> customClasspath, Path pom, ExtractorType extractorType,
	                RoseauOptions.Exclude exclusions) {
		this.location = location.toAbsolutePath();
		this.customClasspath = Set.copyOf(customClasspath);
		this.pom = pom;
		this.extractorType = extractorType;
		this.exclusions = exclusions;
		this.classpath = Suppliers.memoize(() -> {
			if (pom != null && Files.isRegularFile(pom)) {
				MavenClasspathBuilder builder = new MavenClasspathBuilder();
				return Stream.concat(builder.buildClasspath(pom).stream(), this.customClasspath.stream())
					.collect(Collectors.toSet());
			} else {
				return this.customClasspath;
			}
		});
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
	 * @param location the physical location of the library, either a source directory, a JAR file, or a
	 *                 {@code module-info.java} file
	 * @return a new library instance
	 */
	public static Library of(Path location) {
		return new Builder().location(location).build();
	}

	public Path getLocation() {
		return location;
	}

	/**
	 * @return the user-defined classpath
	 */
	public Set<Path> getCustomClasspath() {
		return customClasspath;
	}

	/**
	 * @return the resolved classpath, including custom classpath and pom-inferred classpath
	 */
	public Set<Path> getClasspath() {
		return classpath.get();
	}

	public Path getPom() {
		return pom;
	}

	public ExtractorType getExtractorType() {
		return extractorType;
	}

	public boolean isJar() {
		return isJar(location);
	}

	public boolean isSources() {
		return isSources(location);
	}

	private static boolean isJar(Path file) {
		if (file == null || !Files.isRegularFile(file)) {
			return false;
		}

		// Just read the entries; even on stupidly huge JARs, this is fine
		try (ZipFile zf = new ZipFile(file.toFile())) {
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean isSources(Path file) {
		return file != null && Files.isDirectory(file);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Library other = (Library) obj;
		return Objects.equals(location, other.location) &&
			Objects.equals(customClasspath, other.customClasspath) &&
			Objects.equals(pom, other.pom) &&
			extractorType == other.extractorType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(location, customClasspath, pom, extractorType);
	}

	@Override
	public String toString() {
		return "Library[location=%s, extractor=%s, classpath=%s, pom=%s, excludes=%s]".formatted(
			location, extractorType, customClasspath, pom, exclusions);
	}

	/**
	 * Builder class for constructing {@link Library} instances. Use the provided methods to set the physical location,
	 * classpath, {@code pom.xml} file, and {@link ExtractorType} of the library, and invoke {@link #build()} to create
	 * the corresponding {@link Library}. Only the library's location is required.
	 */
	public static final class Builder {
		private Path location;
		private Set<Path> classpath = Set.of();
		private Path pom;
		private ExtractorType extractorType;
		private RoseauOptions.Exclude exclusions;

		private Builder() {

		}

		/**
		 * Sets the library's physical location, either a source directory, a JAR file, or a {@code module-info.java} file
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
		public Builder classpath(Set<Path> classpath) {
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

		/**
		 * Sets the names/types and annotations to exclude from the API.
		 *
		 * @param exclusions the excludes
		 * @return this builder
		 */
		public Builder exclusions(RoseauOptions.Exclude exclusions) {
			this.exclusions = exclusions;
			return this;
		}

		private static boolean isValidLocation(Path location) {
			return isModuleInfo(location) || isSources(location) || isJar(location);
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
				// short-circuits after the 2nd match
				return s.limit(2L).count() > 1L;
			} catch (IOException e) {
				return false;
			}
		}

		private static boolean isValidPom(Path pom) {
			return pom != null && Files.isRegularFile(pom) && pom.toString().endsWith(".xml");
		}

		/**
		 * Constructs and returns a new {@link Library} instance based on the parameters set in the builder.
		 *
		 * @return the new instance
		 * @throws IllegalArgumentException if the library's location is invalid, if the POM file path is invalid, or if the
		 *                                  specified extractor type is incompatible with the library's type
		 */
		public Library build() {
			if (!isValidLocation(location)) {
				throw new RoseauException("Invalid path to library; directory or JAR expected: " + location);
			}

			if (isModuleInfo(location)) {
				location = location.getParent();
			}

			if (pom != null && !isValidPom(pom)) {
				throw new RoseauException("Invalid path to POM file: " + pom);
			}

			if (isSources(location) && hasMultipleModuleInfo(location)) {
				throw new RoseauException("A library cannot contain multiple module-info.java");
			}

			// Default extractors
			if (extractorType == null) {
				if (isSources(location)) {
					extractorType = ExtractorType.JDT;
				} else {
					extractorType = ExtractorType.ASM;
				}
			}

			if (extractorType == ExtractorType.ASM && isSources(location)) {
				throw new RoseauException("ASM extractor cannot be used on source directories and module-info.java");
			}

			if ((extractorType == ExtractorType.SPOON || extractorType == ExtractorType.JDT) && isJar(location)) {
				throw new RoseauException("Source extractors cannot be used on JARs");
			}

			return new Library(location, classpath, pom, extractorType, exclusions);
		}
	}
}
