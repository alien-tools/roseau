package io.github.alien.roseau.git;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.alien.roseau.options.RoseauOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Entry point for batch repository analysis. Loads repository configurations from a YAML file
 * and delegates to {@link GitWalker} for each configured repository.
 */
public final class BatchGitWalker {
	private static final Logger LOGGER = LoggerFactory.getLogger(BatchGitWalker.class);
	private static final ObjectMapper MAPPER = createMapper();

	private BatchGitWalker() {
	}

	static void main(String[] args) throws IOException {
		if (args.length < 2) {
			LOGGER.error("Usage: BatchGitWalker <config.yaml> <output-dir> [<threads>]");
			System.exit(1);
		}

		int threads = args.length > 2 ? Integer.parseInt(args[2]) : Runtime.getRuntime().availableProcessors();
		walkAll(Path.of(args[0]), Path.of(args[1]), threads);
	}

	/**
	 * Walks every repository declared in {@code yamlConfig}, writing one pair of CSV reports per library into
	 * {@code outputDir}. A failure on one library is logged and does not stop the others.
	 *
	 * @param yamlConfig the YAML configuration listing the repositories to walk
	 * @param outputDir  the directory the CSV reports are written to
	 * @param threads    the number of threads to use
	 */
	private static void walkAll(Path yamlConfig, Path outputDir, int threads) throws IOException {
		List<GitWalker.Config> repos = loadConfig(yamlConfig);

		// Several libraries may be published from a single repository (log4j-api and log4j-core, for instance). They
		// share a working tree, so walking them concurrently makes their checkouts fight over the index lock: group by
		// repository, run the groups concurrently and each group's libraries sequentially.
		Collection<List<GitWalker.Config>> groups = repos.stream()
			.collect(Collectors.groupingBy(GitWalker.Config::gitDir))
			.values();

		LOGGER.info("Walking {} repositories ({} libraries) with {} threads",
			groups.size(), repos.size(), threads);

		try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
			for (List<GitWalker.Config> group : groups) {
				executor.execute(() -> group.forEach(repo -> walkOne(repo, outputDir)));
			}
		}
	}

	private static void walkOne(GitWalker.Config config, Path outputDir) {
		try (CsvReporter reporter = new CsvReporter(config, outputDir)) {
			new GitWalker(config).walk(reporter);
		} catch (Exception e) {
			LOGGER.error("Analysis of {} ({}) failed", config.libraryId(), config.url(), e);
		}
	}

	static List<GitWalker.Config> loadConfig(Path yamlFile) throws IOException {
		JsonNode root = MAPPER.readTree(yamlFile.toFile());
		JsonNode repositoriesNode = root.path("repositories");
		if (!repositoriesNode.isArray()) {
			throw new IOException("%s does not declare a 'repositories' list".formatted(yamlFile));
		}

		JsonNode defaultsNode = root.path("defaults");
		RoseauOptions.Exclude defaultExclusions = sanitizeExclusions(defaultsNode.has("exclusions")
			? MAPPER.convertValue(defaultsNode.get("exclusions"), RoseauOptions.Exclude.class)
			: GitWalker.NO_EXCLUSIONS);
		List<GitWalker.Config> repositories;
		try {
			repositories = MAPPER.convertValue(repositoriesNode, new TypeReference<>() {});
		} catch (IllegalArgumentException e) {
			Throwable rootCause = e;
			while (rootCause.getCause() != null) {
				rootCause = rootCause.getCause();
			}
			throw new IOException("Invalid repository declaration in %s: %s"
				.formatted(yamlFile, rootCause.getMessage()), e);
		}

		List<GitWalker.Config> configs = repositories.stream()
			.map(repo -> repoWithMergedExclusions(repo, defaultExclusions))
			.toList();

		Set<String> duplicates = configs.stream()
			.collect(Collectors.groupingBy(GitWalker.Config::libraryId, Collectors.counting()))
			.entrySet().stream()
			.filter(e -> e.getValue() > 1L)
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());
		if (!duplicates.isEmpty()) {
			throw new IOException("%s declares duplicate libraryId values: %s".formatted(yamlFile, duplicates));
		}

		return configs;
	}

	private static GitWalker.Config repoWithMergedExclusions(GitWalker.Config repo, RoseauOptions.Exclude defaults) {
		RoseauOptions.Exclude exclusions = mergeExclusions(defaults, sanitizeExclusions(repo.exclusions()));
		return new GitWalker.Config(repo.libraryId(), repo.url(), repo.gitDir(), repo.sourceRoots(), exclusions,
			repo.startSha(), repo.endSha());
	}

	private static RoseauOptions.Exclude mergeExclusions(RoseauOptions.Exclude defaults, RoseauOptions.Exclude repo) {
		List<String> mergedNames = Stream.concat(defaults.names().stream(), repo.names().stream()).toList();
		List<RoseauOptions.AnnotationExclusion> mergedAnnotations =
			Stream.concat(defaults.annotations().stream(), repo.annotations().stream()).toList();
		return new RoseauOptions.Exclude(mergedNames, mergedAnnotations);
	}

	private static RoseauOptions.Exclude sanitizeExclusions(RoseauOptions.Exclude exclude) {
		if (exclude == null) {
			return GitWalker.NO_EXCLUSIONS;
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
		// sourceRoots entries are either groups or a single directory
		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		return mapper;
	}
}
