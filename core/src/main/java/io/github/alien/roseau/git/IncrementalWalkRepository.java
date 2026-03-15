package io.github.alien.roseau.git;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.alien.roseau.options.RoseauOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for batch repository analysis. Loads configuration from a YAML file
 * and delegates to {@link GitWalker} for each configured repository.
 */
public final class IncrementalWalkRepository {
	private static final Logger LOGGER = LogManager.getLogger(IncrementalWalkRepository.class);
	private static final RoseauOptions.Exclude EMPTY_EXCLUDE = new RoseauOptions.Exclude(List.of(), List.of());
	private static final ObjectMapper MAPPER = createMapper();

	record Repository(
		String id,
		String url,
		Path gitDir,
		List<Path> sourceRoots,
		Path outputDir,
		RoseauOptions.Exclude exclusions
	) {
	}

	private IncrementalWalkRepository() {
	}

	static void main() throws Exception {
		Path config = Path.of("walk.yaml");
		List<Repository> repos = loadConfig(config);
		repos.parallelStream().forEach(repo -> {
			try {
				new GitWalker(new GitWalker.Config(
					repo.id(), repo.url(), repo.gitDir(), repo.sourceRoots(), repo.exclusions()
				)).walkToCsv(repo.outputDir());
			} catch (Exception e) {
				LOGGER.error("Analysis of {} failed", repo.url(), e);
			}
		});
	}

	// --- YAML config loading ---

	static List<Repository> loadConfig(Path yamlFile) throws IOException {
		JsonNode root = MAPPER.readTree(yamlFile.toFile());
		if (root.isArray()) {
			List<Repository> repositories = MAPPER.convertValue(root, new TypeReference<>() {
			});
			return repositories.stream().map(IncrementalWalkRepository::withDefaults).toList();
		}

		JsonNode defaultsNode = root.path("defaults");
		RoseauOptions.Exclude defaultExclusions = defaultsNode.has("exclusions")
			? MAPPER.convertValue(defaultsNode.get("exclusions"), RoseauOptions.Exclude.class)
			: EMPTY_EXCLUDE;
		List<Repository> repositories = MAPPER.convertValue(root.path("repositories"), new TypeReference<>() {
		});

		return repositories.stream()
			.map(IncrementalWalkRepository::withDefaults)
			.map(repo -> repoWithMergedExclusions(repo, defaultExclusions))
			.toList();
	}

	private static Repository withDefaults(Repository repo) {
		RoseauOptions.Exclude exclusions = sanitizeExclude(repo.exclusions());
		Path outputDir = repo.outputDir() == null ? Path.of(".") : repo.outputDir();
		return new Repository(repo.id(), repo.url(), repo.gitDir(), repo.sourceRoots(), outputDir, exclusions);
	}

	private static Repository repoWithMergedExclusions(Repository repo, RoseauOptions.Exclude defaults) {
		RoseauOptions.Exclude merged = mergeExclusions(defaults, repo.exclusions());
		return new Repository(repo.id(), repo.url(), repo.gitDir(), repo.sourceRoots(), repo.outputDir(), merged);
	}

	private static RoseauOptions.Exclude mergeExclusions(RoseauOptions.Exclude defaults, RoseauOptions.Exclude local) {
		RoseauOptions.Exclude safeDefaults = sanitizeExclude(defaults);
		RoseauOptions.Exclude safeLocal = sanitizeExclude(local);
		List<String> mergedNames = new ArrayList<>();
		mergedNames.addAll(safeDefaults.names());
		mergedNames.addAll(safeLocal.names());
		List<RoseauOptions.AnnotationExclusion> mergedAnnotations = new ArrayList<>();
		mergedAnnotations.addAll(safeDefaults.annotations());
		mergedAnnotations.addAll(safeLocal.annotations());
		return new RoseauOptions.Exclude(List.copyOf(mergedNames), List.copyOf(mergedAnnotations));
	}

	private static RoseauOptions.Exclude sanitizeExclude(RoseauOptions.Exclude exclude) {
		if (exclude == null) {
			return EMPTY_EXCLUDE;
		}
		List<String> names = exclude.names() == null ? List.of() : exclude.names();
		List<RoseauOptions.AnnotationExclusion> annotations = exclude.annotations() == null ? List.of() : exclude.annotations();
		return new RoseauOptions.Exclude(names, annotations);
	}

	private static ObjectMapper createMapper() {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		SimpleModule pathModule = new SimpleModule();
		pathModule.addDeserializer(Path.class,
			new com.fasterxml.jackson.databind.JsonDeserializer<>() {
				@Override
				public Path deserialize(com.fasterxml.jackson.core.JsonParser p,
				                        com.fasterxml.jackson.databind.DeserializationContext ctxt)
					throws IOException {
					return Path.of(p.getValueAsString());
				}
			});
		om.registerModule(pathModule);
		return om;
	}
}
