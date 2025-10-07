package io.github.alien.roseau;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.extractors.ExtractorType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record RoseauOptions(Common common, Library v1, Library v2, Path ignore, List<Report> reports) {
	public record Common(ExtractorType extractor, Classpath classpath, Exclude excludes) {
		Common mergeWith(Common other) {
			return other != null
				? new Common(either(other.extractor(), extractor), classpath.mergeWith(other.classpath()),
					excludes.mergeWith(other.excludes()))
				: this;
		}
	}

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

	public record Classpath(Path pom, Set<Path> jars) {
		Classpath mergeWith(Classpath other) {
			return other != null
				? new Classpath(either(other.pom(), pom), either(other.jars(), jars))
				: this;
		}
	}

	public record Exclude(List<String> names, List<AnnotationExclusion> annotations) {
		Exclude mergeWith(Exclude other) {
			return other != null
				? new Exclude(either(other.names(), names), either(other.annotations(), annotations))
				: this;
		}
	}

	public record AnnotationExclusion(String name, Map<String, String> args) {
	}

	public record Report(Path file, BreakingChangesFormatterFactory format) {

	}

	private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

	public static RoseauOptions load(Path yaml) {
		try {
			return newDefault().mergeWith(MAPPER.readValue(yaml.toFile(), RoseauOptions.class));
		} catch (IOException e) {
			throw new RoseauException("Couldn't read options file %s".formatted(yaml), e);
		}
	}

	public static RoseauOptions newDefault() {
		Classpath defaultClasspath = new Classpath(null, Set.of());
		Exclude defaultExclusion = new Exclude(List.of(), List.of());
		Library defaultLibrary = new Library(null, null, defaultClasspath, defaultExclusion, null);
		Common defaultCommon = new Common(null, defaultClasspath, defaultExclusion);
		List<Report> defaultReports = List.of();
		return new RoseauOptions(defaultCommon, defaultLibrary, defaultLibrary, null, defaultReports);
	}

	public RoseauOptions mergeWith(RoseauOptions other) {
		return other != null
			? new RoseauOptions(common.mergeWith(other.common()), v1.mergeWith(other.v1()), v2.mergeWith(other.v2()),
			either(other.ignore(), ignore), either(other.reports(), reports))
			: this;
	}

	private static <T> T either(T first, T second) {
		return first != null ? first : second;
	}

	private static <T> List<T> either(List<T> first, List<T> second) {
		return first != null && !first.isEmpty() ? first : second;
	}

	private static <T> Set<T> either(Set<T> first, Set<T> second) {
		return first != null && !first.isEmpty() ? first : second;
	}
}
