package io.github.alien.roseau.git;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.alien.roseau.options.RoseauOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Entry point for batch repository analysis. Loads repository configurations from a YAML file
 * and delegates to {@link GitWalker} for each configured repository.
 */
public final class BatchGitWalker {
	private static final Logger LOGGER = LogManager.getLogger(BatchGitWalker.class);
	private static final RoseauOptions.Exclude EMPTY_EXCLUDE = new RoseauOptions.Exclude(List.of(), List.of());
	private static final ObjectMapper MAPPER = createMapper();

	private BatchGitWalker() {
	}

	static void main() throws IOException {
		Path yamlConfig = Path.of("walk.yaml");
		Path outputDir = Path.of("walk-results");
		List<GitWalker.Config> repos = loadConfig(yamlConfig);

		repos.parallelStream().forEach(repo -> {
			GitWalker.Config config = new GitWalker.Config(
				repo.libraryId(), repo.url(), repo.gitDir(), repo.sourceRoots(), repo.exclusions());

			try (CsvReporter reporter = new CsvReporter(config, outputDir)) {
				new GitWalker(config).walk(reporter);
			} catch (Exception e) {
				LOGGER.error("Analysis of {} failed", repo.url(), e);
			}
		});
	}

	static List<GitWalker.Config> loadConfig(Path yamlFile) throws IOException {
		JsonNode root = MAPPER.readTree(yamlFile.toFile());
		JsonNode defaultsNode = root.path("defaults");
		RoseauOptions.Exclude defaultExclusions = sanitizeExclusions(defaultsNode.has("exclusions")
			? MAPPER.convertValue(defaultsNode.get("exclusions"), RoseauOptions.Exclude.class)
			: EMPTY_EXCLUDE);
		List<GitWalker.Config> repositories = MAPPER.convertValue(root.path("repositories"), new TypeReference<>() {
		});

		return repositories.stream()
			.map(repo -> repoWithMergedExclusions(repo, defaultExclusions))
			.toList();
	}

	private static GitWalker.Config repoWithMergedExclusions(GitWalker.Config repo, RoseauOptions.Exclude defaults) {
		RoseauOptions.Exclude exclusions = mergeExclusions(defaults, sanitizeExclusions(repo.exclusions()));
		return new GitWalker.Config(repo.libraryId(), repo.url(), repo.gitDir(), repo.sourceRoots(), exclusions);
	}

	private static RoseauOptions.Exclude mergeExclusions(RoseauOptions.Exclude defaults, RoseauOptions.Exclude repo) {
		List<String> mergedNames = Stream.concat(defaults.names().stream(), repo.names().stream()).toList();
		List<RoseauOptions.AnnotationExclusion> mergedAnnotations =
			Stream.concat(defaults.annotations().stream(), repo.annotations().stream()).toList();
		return new RoseauOptions.Exclude(mergedNames, mergedAnnotations);
	}

	private static RoseauOptions.Exclude sanitizeExclusions(RoseauOptions.Exclude exclude) {
		if (exclude == null) {
			return EMPTY_EXCLUDE;
		}
		List<String> names = exclude.names() == null
			? List.of()
			: exclude.names();
		List<RoseauOptions.AnnotationExclusion> annotations = exclude.annotations() == null
			? List.of()
			: exclude.annotations();
		return new RoseauOptions.Exclude(names, annotations);
	}

	private static ObjectMapper createMapper() {
		SimpleModule pathModule = new SimpleModule();
		pathModule.addDeserializer(Path.class, new JsonDeserializer<>() {
			@Override
			public Path deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
				return Path.of(jsonParser.getValueAsString());
			}
		});
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.registerModule(pathModule);
		return mapper;
	}
}
