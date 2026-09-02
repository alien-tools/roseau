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
	private static final Logger LOGGER = LogManager.getLogger(BatchGitWalker.class);
	private static final RoseauOptions.Exclude EMPTY_EXCLUDE = new RoseauOptions.Exclude(List.of(), List.of());
	private static final ObjectMapper MAPPER = createMapper();

	private BatchGitWalker() {
	}

	private static final Path DEFAULT_CONFIG = Path.of("walk.yaml");
	private static final Path DEFAULT_OUTPUT_DIR = Path.of("walk-results");

	public static void main(String[] args) throws IOException, InterruptedException {
		Path yamlConfig = args.length > 0 ? Path.of(args[0]) : DEFAULT_CONFIG;
		Path outputDir = args.length > 1 ? Path.of(args[1]) : DEFAULT_OUTPUT_DIR;
		walkAll(yamlConfig, outputDir);
	}

	/**
	 * Walks every repository declared in {@code yamlConfig}, writing one pair of CSV reports per library into
	 * {@code outputDir}. A failure on one library is logged and does not stop the others.
	 *
	 * @param yamlConfig the YAML configuration listing the repositories to walk
	 * @param outputDir  the directory the CSV reports are written to
	 */
	public static void walkAll(Path yamlConfig, Path outputDir) throws IOException, InterruptedException {
		List<GitWalker.Config> repos = loadConfig(yamlConfig);

		// Several libraries may be published from a single repository (log4j-api and log4j-core, for instance). They
		// share a working tree, so walking them concurrently makes their checkouts fight over the index lock: group by
		// repository, run the groups concurrently and each group's libraries sequentially.
		Collection<List<GitWalker.Config>> groups = repos.stream()
			.collect(Collectors.groupingBy(GitWalker.Config::gitDir))
			.values();

		// A walk is long-running and mixes blocking Git I/O with CPU-bound parsing, and each concurrent walk holds a
		// full API model in memory. Virtual threads would buy nothing here and would let memory grow with the number
		// of repositories, so the pool is bounded by the available processors instead.
		int concurrency = Math.min(groups.size(), Runtime.getRuntime().availableProcessors());
		LOGGER.info("Walking {} repositories ({} libraries) with a concurrency of {}",
			groups.size(), repos.size(), concurrency);

		try (ExecutorService executor = Executors.newFixedThreadPool(concurrency)) {
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
			: EMPTY_EXCLUDE);
		List<GitWalker.Config> repositories;
		try {
			repositories = MAPPER.convertValue(repositoriesNode, new TypeReference<>() {
			});
		} catch (IllegalArgumentException e) {
			// Jackson swallows the constructor's message, so surface the root cause explicitly
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
			.filter(e -> e.getValue() > 1)
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());
		if (!duplicates.isEmpty()) {
			// Reports are named after the library, so duplicates would silently overwrite each other
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
