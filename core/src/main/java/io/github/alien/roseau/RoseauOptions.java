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

public record RoseauOptions(Diff diff) {
	public record Diff(Library v1, Library v2, Path ignore, List<Report> reports) {
		public Diff mergeWith(Diff other) {
			if (other == null) {
				return this;
			}
			return new Diff(v1.mergeWith(other.v1()), v2.mergeWith(other.v2()),
				either(other.ignore(), ignore), either(other.reports(), reports));
		}
	}

	public record Library(Path location, ExtractorType extractor, Classpath classpath, Exclude excludes,
	                      Path apiReport) {
		public Library mergeWith(Library other) {
			if (other == null) {
				return this;
			}
			return new Library(
				either(other.location(), location),
				either(other.extractor(), extractor),
				classpath.mergeWith(other.classpath()),
				excludes.mergeWith(other.excludes()),
				either(other.apiReport(), apiReport)
			);
		}
	}

	public record Classpath(Path pom, Set<Path> jars) {
		public Classpath mergeWith(Classpath other) {
			if (other == null) {
				return this;
			}
			return new Classpath(either(other.pom(), pom), either(other.jars(), jars));
		}
	}

	public record Exclude(List<String> names, List<AnnotationExclusion> annotations) {
		public Exclude mergeWith(Exclude other) {
			if (other == null) {
				return this;
			}
			return new Exclude(either(other.names(), names), either(other.annotations(), annotations));
		}
	}

	public record AnnotationExclusion(String name, Map<String, String> args) {
	}

	public record Report(Path file, BreakingChangesFormatterFactory format) {}

	private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

	public static RoseauOptions load(Path yamlPath) {
		try {
			return newDefault().mergeWith(MAPPER.readValue(yamlPath.toFile(), RoseauOptions.class));
		} catch (IOException e) {
			throw new RoseauException("Couldn't read options file %s".formatted(yamlPath), e);
		}
	}

	public static RoseauOptions newDefault() {
		Classpath defaultClasspath = new Classpath(null, Set.of());
		Exclude defaultExclusion = new Exclude(List.of(), List.of());
		Library defaultLibrary = new Library(null, null, defaultClasspath, defaultExclusion, null);
		List<Report> defaultReports = List.of();
		return new RoseauOptions(new Diff(defaultLibrary, defaultLibrary, null, defaultReports));
	}

	public RoseauOptions mergeWith(RoseauOptions other) {
		return new RoseauOptions(diff.mergeWith(other.diff()));
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
