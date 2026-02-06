package io.github.alien.roseau;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;

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
 * @param diff    diff options
 * @param reports reports configuration
 */
public record RoseauOptions(Common common, Library v1, Library v2, Diff diff, List<Report> reports) {
	/**
	 * Options shared by v1 and v2.
	 *
	 * @param classpath the {@link Classpath} to use
	 * @param excludes  the API {@link Exclude} options to apply
	 */
	public record Common(Classpath classpath, Exclude excludes) {
		Common mergeWith(Common other) {
			return other != null
				? new Common(classpath.mergeWith(other.classpath()),
				excludes.mergeWith(other.excludes()))
				: this;
		}
	}

	/**
	 * Options for a particular library, v1 or v2.
	 *
	 * @param location      the location of the library
	 * @param classpath     the {@link Classpath} to use
	 * @param excludes      the API {@link Exclude} options to apply
	 * @param apiReport     the location of the JSON API report to generate
	 * @param apiHtmlReport the location of the HTML API report to generate
	 */
	public record Library(Path location, Classpath classpath, Exclude excludes, Path apiReport, Path apiHtmlReport) {
		Library mergeWith(Library other) {
			return other != null
				? new Library(either(other.location(), location), classpath.mergeWith(other.classpath()),
				excludes.mergeWith(other.excludes()), either(other.apiReport(), apiReport),
				either(other.apiHtmlReport(), apiHtmlReport))
				: this;
		}

		public Library mergeWith(Common common) {
			return common != null
				? new Library(location, common.classpath().mergeWith(classpath),
				common.excludes().mergeWith(excludes), apiReport, apiHtmlReport)
				: this;
		}

		public io.github.alien.roseau.Library toLibrary() {
			return io.github.alien.roseau.Library.builder()
				.location(location)
				.classpath(classpath.jars())
				.pom(classpath.pom())
				.exclusions(excludes)
				.build();
		}
	}

	/**
	 * Diff options.
	 *
	 * @param ignore     the CSV "ignore" file to use
	 * @param sourceOnly whether to report source-breaking changes only
	 * @param binaryOnly whether to report binary-breaking changes only
	 */
	public record Diff(Path ignore, Boolean sourceOnly, Boolean binaryOnly) {
		Diff mergeWith(Diff other) {
			return other != null
				? new Diff(either(other.ignore(), ignore), either(other.sourceOnly(), either(sourceOnly, false)),
					either(other.binaryOnly(), either(binaryOnly, false)))
				: this;
		}
	}

	/**
	 * A classpath configuration for a library.
	 *
	 * @param pom  the path to the {@code pom.xml} to extract a classpath from
	 * @param jars the manual classpath
	 */
	public record Classpath(Path pom, List<Path> jars) {
		Classpath mergeWith(Classpath other) {
			return other != null
				? new Classpath(either(other.pom(), pom), either(other.jars(), jars))
				: this;
		}
	}

	/**
	 * Exclusion options when reporting breaking changes.
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
	 * A particular code annotation that ignores breaking changes on annotated symbols
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

	static {
		// Force Jackson to initialize absent values as empty collections, not null
		MAPPER.configOverride(List.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
		MAPPER.configOverride(Set.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
		MAPPER.configOverride(Map.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
	}

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
		Classpath defaultClasspath = new Classpath(null, List.of());
		Exclude defaultExclusion = new Exclude(List.of(), List.of());
		Library defaultLibrary = new Library(null, defaultClasspath, defaultExclusion, null, null);
		Common defaultCommon = new Common(defaultClasspath, defaultExclusion);
		Diff diff = new Diff(null, false, false);
		List<Report> defaultReports = List.of();
		return new RoseauOptions(defaultCommon, defaultLibrary, defaultLibrary, diff, defaultReports);
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
			diff.mergeWith(other.diff()), either(other.reports(), reports))
			: this;
	}

	private static <T> T either(T first, T second) {
		return first != null ? first : second;
	}

	private static <T, C extends Collection<T>> C either(C first, C second) {
		return first != null && !first.isEmpty() ? first : second;
	}
}
