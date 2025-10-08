package io.github.alien.roseau;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.extractors.ExtractorType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Roseau's configuration options.
 *
 * @param common  options shared by v1 and v2
 * @param v1      options specific to v1
 * @param v2      options specific to v2
 * @param ignore  the CSV "ignore" file to use
 * @param reports reports configuration
 */
public record RoseauOptions(Common common, Library v1, Library v2, Path ignore, List<Report> reports) {
	/**
	 * Options shared by v1 and v2
	 *
	 * @param extractor the {@link ExtractorType} to use
	 * @param classpath the {@link Classpath} to use
	 * @param excludes  the API {@link Exclude} options to apply
	 */
	public record Common(ExtractorType extractor, Classpath classpath, Exclude excludes) {
		Common mergeWith(Common other) {
			return other != null
				? new Common(either(other.extractor(), extractor), classpath.mergeWith(other.classpath()),
				excludes.mergeWith(other.excludes()))
				: this;
		}
	}

	/**
	 * Options for a particular library, v1 or v2
	 *
	 * @param location  the location of the library
	 * @param extractor the {@link ExtractorType} to use
	 * @param classpath the {@link Classpath} to use
	 * @param excludes  the API {@link Exclude} options to apply
	 * @param apiReport the location of the API report to generate
	 */
	public record Library(Path location, ExtractorType extractor, Classpath classpath, Exclude excludes, Path apiReport) {
		Library mergeWith(Library other) {
			return other != null
				? new Library(either(other.location(), location), either(other.extractor(), extractor),
				classpath.mergeWith(other.classpath()), excludes.mergeWith(other.excludes()),
				either(other.apiReport(), apiReport))
				: this;
		}

		public Library mergeWith(Common common) {
			return common != null
				? new Library(location, either(extractor, common.extractor()), common.classpath().mergeWith(classpath),
				common.excludes().mergeWith(excludes), apiReport)
				: this;
		}

		public io.github.alien.roseau.Library toLibrary() {
			return io.github.alien.roseau.Library.builder()
				.location(location)
				.classpath(classpath.jars())
				.pom(classpath.pom())
				.extractorType(extractor)
				.exclusions(excludes)
				.build();
		}
	}

	/**
	 * A classpath configuration for a library.
	 *
	 * @param pom  the path to the {@code pom.xml} to extract a classpath from
	 * @param jars the manual classpath
	 */
	public record Classpath(Path pom, Set<Path> jars) {
		Classpath mergeWith(Classpath other) {
			return other != null
				? new Classpath(either(other.pom(), pom), either(other.jars(), jars))
				: this;
		}
	}

	/**
	 * Exclusion options to apply to API inference.
	 *
	 * @param names       the list of regex-based names to be excluded
	 * @param annotations the list of {@link AnnotationExclusion} to consider
	 */
	public record Exclude(List<String> names, List<AnnotationExclusion> annotations) {
		Exclude mergeWith(Exclude other) {
			return other != null
				? new Exclude(either(other.names(), names), either(other.annotations(), annotations))
				: this;
		}
	}

	/**
	 * A particular code annotation that excludes the symbols it's applied to from the API
	 *
	 * @param name the fully qualified name of the annotation
	 * @param args the argument values, if any
	 */
	public record AnnotationExclusion(String name, Map<String, String> args) {
	}

	/**
	 * A report configuration for breaking changes.
	 *
	 * @param file   the path to the file where the report will be saved
	 * @param format the format to apply when generating the report
	 */
	public record Report(Path file, BreakingChangesFormatterFactory format) {

	}

	private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

	/**
	 * Creates a new instance from the given YAML file.
	 *
	 * @param yaml the path to the YAML file containing the configuration
	 * @return an instance representing the options
	 * @throws RoseauException if an error occurs while reading or parsing the file
	 */
	public static RoseauOptions load(Path yaml) {
		try {
			return newDefault().mergeWith(MAPPER.readValue(yaml.toFile(), RoseauOptions.class));
		} catch (IOException e) {
			throw new RoseauException("Couldn't read options file %s".formatted(yaml), e);
		}
	}

	/**
	 * A default empty instance.
	 *
	 * @return the empty instance
	 */
	public static RoseauOptions newDefault() {
		Classpath defaultClasspath = new Classpath(null, Set.of());
		Exclude defaultExclusion = new Exclude(List.of(), List.of());
		Library defaultLibrary = new Library(null, null, defaultClasspath, defaultExclusion, null);
		Common defaultCommon = new Common(null, defaultClasspath, defaultExclusion);
		List<Report> defaultReports = List.of();
		return new RoseauOptions(defaultCommon, defaultLibrary, defaultLibrary, null, defaultReports);
	}

	/**
	 * Merges the current instance with another one, overriding any value with those present in {@code other}.
	 *
	 * @param other the {@code RoseauOptions} instance to merge with; may be null
	 * @return a new {@code RoseauOptions} instance that represents the merged result, or the current instance if the
	 * other instance is null
	 */
	public RoseauOptions mergeWith(RoseauOptions other) {
		return other != null
			? new RoseauOptions(common.mergeWith(other.common()), v1.mergeWith(other.v1()), v2.mergeWith(other.v2()),
			either(other.ignore(), ignore), either(other.reports(), reports))
			: this;
	}

	private static <T> T either(T first, T second) {
		return first != null ? first : second;
	}

	private static <T, C extends Collection<T>> C either(C first, C second) {
		return first != null && !first.isEmpty() ? first : second;
	}
}
