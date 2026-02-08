package io.github.alien.roseau.git;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauOptions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.changes.BreakingChangeNature;
import io.github.alien.roseau.extractors.TypesExtractor;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class RepositoryWalkerUtils {
	private static final Logger LOGGER = LogManager.getLogger(RepositoryWalkerUtils.class);
	static final List<String> COMMITS_HEADER = List.of(
		"library",
		"commit_sha",
		"commit_short_msg",
		"conventional_commit_tag",
		"parent_commit",
		"date_utc",
		"is_merge_commit",
		"branch",
		"tag",
		"version",
		"days_since_prev_commit",
		"files_changed",
		"loc_added",
		"loc_deleted",
		"all_api_types_count",
		"all_api_methods_count",
		"all_api_fields_count",
		"all_api_symbols_count",
		"exported_types_count",
		"exported_methods_count",
		"exported_fields_count",
		"deprecated_count",
		"internal_count",
		"breaking_changes_count",
		"binary_breaking_changes_count",
		"source_breaking_changes_count",
		"has_java_changes",
		"has_pom_changes",
		"checkout_time_ms",
		"classpath_time_ms",
		"api_time_ms",
		"diff_time_ms",
		"stats_time_ms"
	);
	static final List<String> BCS_HEADER = List.of(
		"library",
		"commit",
		"kind",
		"nature",
		"details",
		"compatibility",
		"impacted_package_fqn",
		"impacted_type_fqn",
		"impacted_symbol_fqn",
		"symbol_visibility",
		"is_excluded_symbol",
		"is_deprecated_removal",
		"is_internal_removal",
		"source_file",
		"source_line"
	);
	private static final RoseauOptions.Exclude EMPTY_EXCLUDE = new RoseauOptions.Exclude(List.of(), List.of());
	private static final Pattern CONVENTIONAL_COMMIT = Pattern.compile("^([a-zA-Z]+)(?:\\([^)]*\\))?(!)?:\\s+.+$");
	private static final Pattern VERSION_LIKE_TAG = Pattern.compile("^v?\\d+(?:\\.\\d+)*(?:[-+._][\\w.-]+)?$");

	record Repository(
		String id,
		String url,
		Path gitDir,
		List<Path> sourceRoots,
		Path outputDir,
		RoseauOptions.Exclude exclusions
	) {
	}

	record ApiStats(
		int allApiTypesCount,
		int allApiMethodsCount,
		int allApiFieldsCount,
		int exportedTypesCount,
		int exportedMethodsCount,
		int exportedFieldsCount,
		long deprecatedCount,
		long internalCount
	) {
		int allApiSymbolsCount() {
			return allApiTypesCount + allApiMethodsCount + allApiFieldsCount;
		}
	}

	record CommitDiff(
		boolean javaChanged,
		boolean pomChanged,
		int filesChanged,
		int locAdded,
		int locDeleted,
		Set<Path> updatedJavaFiles,
		Set<Path> deletedJavaFiles,
		Set<Path> createdJavaFiles
	) {
	}

	record CommitAnalysis(
		CommitDiff commitDiff,
		ApiStats apiStats,
		int breakingChangesCount,
		int binaryBreakingChangesCount,
		int sourceBreakingChangesCount,
		long checkoutTimeMs,
		long classpathTimeMs,
		long apiTimeMs,
		long diffTimeMs,
		long statsTimeMs
	) {
	}

	record OutputFiles(Path commitsCsv, Path bcsCsv) {
	}

	record ExclusionMatcher(
		List<Pattern> namePatterns,
		List<RoseauOptions.AnnotationExclusion> annotationExclusions
	) {
		boolean isInternal(Symbol symbol) {
			if (symbol == null) {
				return false;
			}

			boolean excludedByName = namePatterns.stream()
				.anyMatch(pattern -> pattern.matcher(symbol.getQualifiedName()).matches());
			if (excludedByName) {
				return true;
			}

			for (RoseauOptions.AnnotationExclusion exclusion : annotationExclusions) {
				if (symbol.getAnnotations().stream().anyMatch(ann -> annotationMatches(ann, exclusion))) {
					return true;
				}
			}

			return false;
		}
	}

	private static final ObjectMapper MAPPER = createMapper();

	private RepositoryWalkerUtils() {
	}

	static Path prepareRepository(String url, Path gitDir) throws Exception {
		Path workTree = workTreeFromGitDir(gitDir);
		if (!Files.exists(gitDir)) {
			LOGGER.info("Local clone not found for {}, cloning into {}", url, workTree);
			cloneRepository(url, workTree);
			return workTree;
		}

		LOGGER.info("Preparing existing clone at {} (remote {})", gitDir, url);
		FileRepositoryBuilder builder = new FileRepositoryBuilder().setGitDir(gitDir.toFile()).readEnvironment();
		try (org.eclipse.jgit.lib.Repository repo = builder.build(); Git git = new Git(repo)) {
			fetchAndAlignRepository(git, repo);
			LOGGER.debug("Resetting and cleaning working tree at {}", repo.getWorkTree());
			makePristine(git);
		} catch (Exception e) {
			if (!isMissingObjectFailure(e)) {
				throw e;
			}
			LOGGER.warn("Repository {} has missing objects, deleting local clone and cloning again", gitDir, e);
			recloneRepository(url, gitDir);
		}
		return workTree;
	}

	static boolean isMissingObjectFailure(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof MissingObjectException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	static Path recloneRepository(String url, Path gitDir) throws Exception {
		Path workTree = workTreeFromGitDir(gitDir);
		LOGGER.info("Deleting existing clone at {} and cloning {} again", workTree, url);
		deleteRecursively(workTree);
		cloneRepository(url, workTree);
		return workTree;
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
		pathModule.addSerializer(Path.class,
			new com.fasterxml.jackson.databind.JsonSerializer<>() {
				@Override
				public void serialize(Path value,
				                      com.fasterxml.jackson.core.JsonGenerator gen,
				                      com.fasterxml.jackson.databind.SerializerProvider serializers)
					throws IOException {
					gen.writeString(value.toString());
				}
			});

		om.registerModule(pathModule);
		return om;
	}

	static List<Repository> loadConfig(Path yamlFile) throws IOException {
		JsonNode root = MAPPER.readTree(yamlFile.toFile());
		if (root.isArray()) {
			List<Repository> repositories = MAPPER.convertValue(
				root,
				new TypeReference<List<Repository>>() {
				}
			);
			return repositories.stream().map(RepositoryWalkerUtils::withDefaultExclusions).toList();
		}

		JsonNode defaultsNode = root.path("defaults");
		RoseauOptions.Exclude defaultExclusions = defaultsNode.has("exclusions")
			? toExclude(defaultsNode.get("exclusions"))
			: EMPTY_EXCLUDE;
		List<Repository> repositories = MAPPER.convertValue(
			root.path("repositories"),
			new TypeReference<List<Repository>>() {
			}
		);

		return repositories.stream()
			.map(RepositoryWalkerUtils::withDefaultExclusions)
			.map(repo -> repoWithMergedExclusions(repo, defaultExclusions))
			.toList();
	}

	static List<RevCommit> firstParentChain(org.eclipse.jgit.lib.Repository repo, RevWalk rw) throws IOException {
		ObjectId headId = repo.resolve("HEAD");
		if (headId == null) {
			throw new IllegalStateException("Cannot resolve HEAD");
		}

		List<RevCommit> chain = new ArrayList<>();
		RevCommit cur = rw.parseCommit(headId);
		while (true) {
			chain.add(cur);
			if (cur.getParentCount() == 0) break;
			cur = rw.parseCommit(cur.getParent(0)); // first parent only
		}
		Collections.reverse(chain); // oldest -> newest
		return chain;
	}

	static CommitDiff computeCommitDiff(org.eclipse.jgit.lib.Repository repo, RevCommit commit) {
		try (RevWalk rw = new RevWalk(repo);
		     ObjectReader reader = repo.newObjectReader();
		     DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

			df.setRepository(repo);
			df.setDetectRenames(false);

			CanonicalTreeParser newTree = new CanonicalTreeParser();
			newTree.reset(reader, commit.getTree().getId());

			CanonicalTreeParser oldTree = new CanonicalTreeParser();
			if (commit.getParentCount() > 0) {
				RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
				oldTree.reset(reader, parent.getTree().getId());
			} else {
				ObjectId emptyTreeId = repo.resolve(Constants.EMPTY_TREE_ID.name());
				oldTree.reset(reader, emptyTreeId);
			}

			List<DiffEntry> diffs = df.scan(oldTree, newTree);

			Set<Path> updated = new HashSet<>();
			Set<Path> deleted = new HashSet<>();
			Set<Path> created = new HashSet<>();
			boolean pomChanged = false;
			int locAdded = 0;
			int locDeleted = 0;

			for (DiffEntry e : diffs) {
				Path oldPath = pathOf(e.getOldPath());
				Path newPath = pathOf(e.getNewPath());

				if (!pomChanged && (isPomPath(oldPath) || isPomPath(newPath))) {
					pomChanged = true;
				}

				switch (e.getChangeType()) {
					case ADD -> addIfJava(created, newPath);
					case MODIFY -> addIfJava(updated, newPath);
					case DELETE -> addIfJava(deleted, oldPath);
					case RENAME -> {
						addIfJava(deleted, oldPath);
						addIfJava(created, newPath);
					}
					case COPY -> addIfJava(created, newPath);
				}

				for (Edit edit : df.toFileHeader(e).toEditList()) {
					locAdded += edit.getEndB() - edit.getBeginB();
					locDeleted += edit.getEndA() - edit.getBeginA();
				}
			}

			boolean javaChanged = !updated.isEmpty() || !deleted.isEmpty() || !created.isEmpty();
			return new CommitDiff(
				javaChanged,
				pomChanged,
				diffs.size(),
				locAdded,
				locDeleted,
				Set.copyOf(updated),
				Set.copyOf(deleted),
				Set.copyOf(created)
			);
		} catch (Exception _) {
			return new CommitDiff(true, true, 0, 0, 0, Set.of(), Set.of(), Set.of());
		}
	}

	static String defaultBranchName(org.eclipse.jgit.lib.Repository repo) throws Exception {
		return Optional.ofNullable(resolveRemoteBranchName(repo)).orElse("");
	}

	static Optional<Path> sourceRootRelativeToWorkTree(Path workTree, Path sourceRoot) {
		Path absoluteWorkTree = workTree.toAbsolutePath().normalize();
		Path absoluteSourceRoot = sourceRoot.toAbsolutePath().normalize();
		if (!absoluteSourceRoot.startsWith(absoluteWorkTree)) {
			return Optional.empty();
		}
		return Optional.of(absoluteWorkTree.relativize(absoluteSourceRoot));
	}

	static ChangedFiles changedFilesForSourceRoot(CommitDiff diff, Path sourceRootRelative) {
		Set<Path> updated = relativizeUnderRoot(diff.updatedJavaFiles(), sourceRootRelative);
		Set<Path> deleted = relativizeUnderRoot(diff.deletedJavaFiles(), sourceRootRelative);
		Set<Path> created = relativizeUnderRoot(diff.createdJavaFiles(), sourceRootRelative);
		return new ChangedFiles(updated, deleted, created);
	}

	static API buildApi(Path sources, List<Path> classpath, RoseauOptions.Exclude exclusions) {
		Library library = Library.builder()
			.location(sources)
			.classpath(classpath)
			.exclusions(exclusions)
			.build();
		TypesExtractor extractor = new JdtTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
		LibraryTypes types = extractor.extractTypes(library);
		return types.toAPI();
	}

	static ApiStats computeApiStats(API api, ExclusionMatcher exclusionMatcher) {
		int allTypesCount = api.getLibraryTypes().getAllTypes().size();
		int allMethodsCount = api.getLibraryTypes().getAllTypes().stream()
			.mapToInt(type -> type.getDeclaredMethods().size())
			.sum();
		int allFieldsCount = api.getLibraryTypes().getAllTypes().stream()
			.mapToInt(type -> type.getDeclaredFields().size())
			.sum();
		int exportedTypesCount = api.getExportedTypes().size();
		int exportedMethodsCount = api.getExportedTypes().stream()
			.mapToInt(type -> type.getDeclaredMethods().size())
			.sum();
		int exportedFieldsCount = api.getExportedTypes().stream()
			.mapToInt(type -> type.getDeclaredFields().size())
			.sum();
		return new ApiStats(
			allTypesCount,
			allMethodsCount,
			allFieldsCount,
			exportedTypesCount,
			exportedMethodsCount,
			exportedFieldsCount,
			getApiAnnotationsCount(api, "java.lang.Deprecated"),
			getInternalCount(api, exclusionMatcher)
		);
	}

	static OutputFiles resolveOutputFiles(String library, Path outputDir) {
		Path parent = outputDir == null ? Path.of(".") : outputDir;
		LOGGER.debug("Resolved output directory for {} to {}", library, parent.toAbsolutePath().normalize());
		return new OutputFiles(
			parent.resolve(library + "-commits.csv"),
			parent.resolve(library + "-bcs.csv")
		);
	}

	static BufferedWriter openCsvWriter(Path file) throws IOException {
		Path parent = file.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		return Files.newBufferedWriter(file,
			StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	static void writeCsvHeader(Writer writer, List<String> header) throws IOException {
		writeCsvRow(writer, header.stream().map(s -> (Object) s).toList());
	}

	static void writeCommitRow(
		Writer writer,
		String library,
		RevCommit commit,
		String conventionalCommitTag,
		String parentCommit,
		String branch,
		String tags,
		String version,
		long daysSincePreviousCommit,
		CommitAnalysis analysis
	) throws IOException {
		writeCsvRow(writer, List.of(
			library,
			commit.getName(),
			commit.getShortMessage(),
			conventionalCommitTag,
			parentCommit,
			Instant.ofEpochSecond(commit.getCommitTime()).toString(),
			commit.getParentCount() > 1,
			branch,
			tags,
			version,
			daysSincePreviousCommit,
			analysis.commitDiff().filesChanged(),
			analysis.commitDiff().locAdded(),
			analysis.commitDiff().locDeleted(),
			analysis.apiStats().allApiTypesCount(),
			analysis.apiStats().allApiMethodsCount(),
			analysis.apiStats().allApiFieldsCount(),
			analysis.apiStats().allApiSymbolsCount(),
			analysis.apiStats().exportedTypesCount(),
			analysis.apiStats().exportedMethodsCount(),
			analysis.apiStats().exportedFieldsCount(),
			analysis.apiStats().deprecatedCount(),
			analysis.apiStats().internalCount(),
			analysis.breakingChangesCount(),
			analysis.binaryBreakingChangesCount(),
			analysis.sourceBreakingChangesCount(),
			analysis.commitDiff().javaChanged(),
			analysis.commitDiff().pomChanged(),
			analysis.checkoutTimeMs(),
			analysis.classpathTimeMs(),
			analysis.apiTimeMs(),
			analysis.diffTimeMs(),
			analysis.statsTimeMs()
		));
	}

	static void writeBreakingChangesRows(
		Writer writer,
		String library,
		String commitSha,
		API baselineApi,
		List<BreakingChange> breakingChanges,
		ExclusionMatcher internalMatcher
	) throws IOException {
		for (BreakingChange bc : breakingChanges) {
			SourceLocation location = bc.getLocation();
			Symbol impactedSymbol = bc.impactedSymbol();
			BreakingChangeKind kind = bc.kind();
			boolean isExcludedSymbol = baselineApi.isExcluded(impactedSymbol)
				|| baselineApi.isExcluded(bc.impactedType())
				|| internalMatcher.isInternal(impactedSymbol)
				|| internalMatcher.isInternal(bc.impactedType());
			boolean isRemoval = kind.getNature() == BreakingChangeNature.DELETION;
			boolean isDeprecatedRemoval = isRemoval && hasAnnotation(impactedSymbol, "java.lang.Deprecated");
			boolean isInternalRemoval = isRemoval && internalMatcher.isInternal(impactedSymbol);
			writeCsvRow(writer, List.of(
				library,
				commitSha,
				kind.name(),
				kind.getNature().name().toLowerCase(Locale.ROOT),
				bc.details().toString(),
				compatibility(kind),
				bc.impactedType().getPackageName(),
				bc.impactedType().getQualifiedName(),
				impactedSymbol.getQualifiedName(),
				visibility(impactedSymbol.getVisibility()),
				isExcludedSymbol,
				isDeprecatedRemoval,
				isInternalRemoval,
				location.file() != null ? location.file().toString() : "",
				location.line() >= 0 ? location.line() : ""
			));
		}
	}

	static String conventionalCommitTag(String shortMessage) {
		var matcher = CONVENTIONAL_COMMIT.matcher(shortMessage);
		return matcher.matches() ? matcher.group(1).toLowerCase(Locale.ROOT) : "";
	}

	static String parentCommit(RevCommit commit) {
		return commit.getParentCount() > 0 ? commit.getParent(0).getName() : "";
	}

	static long daysSincePreviousCommit(RevCommit previous, RevCommit current) {
		if (previous == null) {
			return 0;
		}
		return ChronoUnit.DAYS.between(
			Instant.ofEpochSecond(previous.getCommitTime()),
			Instant.ofEpochSecond(current.getCommitTime())
		);
	}

	static Map<String, List<String>> tagsByCommit(org.eclipse.jgit.lib.Repository repo) throws IOException {
		Map<String, List<String>> tagsByCommit = new HashMap<>();
		try (RevWalk rw = new RevWalk(repo)) {
			for (Ref ref : repo.getRefDatabase().getRefsByPrefix(Constants.R_TAGS)) {
				Ref peeled = repo.getRefDatabase().peel(ref);
				ObjectId objectId = peeled.getPeeledObjectId() != null ? peeled.getPeeledObjectId() : ref.getObjectId();
				if (objectId == null) {
					continue;
				}
				try {
					RevCommit commit = rw.parseCommit(objectId);
					String tag = ref.getName().substring(Constants.R_TAGS.length());
					tagsByCommit.computeIfAbsent(commit.getName(), _ -> new ArrayList<>()).add(tag);
				} catch (Exception ignored) {
					// Ignore tags not pointing to commits
				}
			}
		}
		tagsByCommit.values().forEach(Collections::sort);
		return tagsByCommit;
	}

	static String joinedTags(Map<String, List<String>> tagsByCommit, String commitSha) {
		return String.join(";", tagsByCommit.getOrDefault(commitSha, List.of()));
	}

	static String resolveVersionFromTags(Map<String, List<String>> tagsByCommit, String commitSha) {
		List<String> tags = tagsByCommit.getOrDefault(commitSha, List.of());
		return tags.stream().filter(tag -> VERSION_LIKE_TAG.matcher(tag).matches()).findFirst()
			.or(() -> tags.stream().findFirst())
			.orElse("");
	}

	static ExclusionMatcher exclusionMatcher(RoseauOptions.Exclude exclusions) {
		List<Pattern> patterns = exclusions.names().stream()
			.map(Pattern::compile)
			.toList();
		return new ExclusionMatcher(List.copyOf(patterns), exclusions.annotations());
	}

	static void makePristine(Git git) throws Exception {
		LOGGER.debug("Resetting repository to HEAD");
		git.reset()
			.setMode(ResetCommand.ResetType.HARD)
			.call();

		LOGGER.debug("Cleaning untracked files and directories");
		git.clean()
			.setCleanDirectories(true)
			.setIgnore(false)
			.call();
	}

	private static Path workTreeFromGitDir(Path gitDir) {
		Path normalized = gitDir.toAbsolutePath().normalize();
		if (normalized.getFileName() != null && ".git".equals(normalized.getFileName().toString())) {
			return normalized.getParent();
		}
		return normalized;
	}

	private static void cloneRepository(String url, Path workTree) throws GitAPIException, IOException {
		if (workTree == null) {
			throw new IOException("Cannot resolve repository work tree");
		}
		Path parent = workTree.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		LOGGER.info("Cloning {} into {}", url, workTree);
		Git.cloneRepository()
			.setURI(url)
			.setDirectory(workTree.toFile())
			.call()
			.close();
	}

	private static void fetchAndAlignRepository(Git git, org.eclipse.jgit.lib.Repository repo) throws Exception {
		LOGGER.info("Fetching updates from origin for {}", repo.getDirectory());
		var fetch = git.fetch().setRemote("origin");
		if (isShallowRepository(repo)) {
			LOGGER.info("Repository {} is shallow, unshallowing during fetch", repo.getDirectory());
			fetch.setUnshallow(true);
		}
		fetch.call();
		alignToRemoteDefaultBranch(git, repo);
	}

	private static void alignToRemoteDefaultBranch(Git git, org.eclipse.jgit.lib.Repository repo) throws Exception {
		String branchName = resolveRemoteBranchName(repo);
		if (branchName == null) {
			LOGGER.warn("Could not resolve default remote branch for {}", repo.getDirectory());
			return;
		}
		LOGGER.info("Aligning local repository to origin/{}", branchName);

		if (repo.findRef("refs/heads/" + branchName) == null) {
			LOGGER.debug("Creating local branch {} from origin/{}", branchName, branchName);
			git.checkout()
				.setCreateBranch(true)
				.setName(branchName)
				.setStartPoint("origin/" + branchName)
				.call();
		} else {
			LOGGER.debug("Checking out existing local branch {}", branchName);
			git.checkout()
				.setName(branchName)
				.setForced(true)
				.call();
		}
		LOGGER.debug("Hard-resetting branch {} to origin/{}", branchName, branchName);
		git.reset()
			.setMode(ResetCommand.ResetType.HARD)
			.setRef("origin/" + branchName)
			.call();
	}

	private static String resolveRemoteBranchName(org.eclipse.jgit.lib.Repository repo) throws Exception {
		final String prefix = "refs/remotes/origin/";

		Ref remoteHead = repo.exactRef("refs/remotes/origin/HEAD");
		if (remoteHead != null && remoteHead.getTarget() != null) {
			String remoteRefName = remoteHead.getTarget().getName();
			if (remoteRefName.startsWith(prefix)) {
				return remoteRefName.substring(prefix.length());
			}
		}

		for (String candidate : List.of("main", "master")) {
			if (repo.findRef(prefix + candidate) != null) {
				return candidate;
			}
		}

		String currentBranch = repo.getBranch();
		if (currentBranch != null && repo.findRef(prefix + currentBranch) != null) {
			return currentBranch;
		}

		return repo.getRefDatabase().getRefsByPrefix(prefix).stream()
			.map(ref -> ref.getName().substring(prefix.length()))
			.filter(name -> !name.equals("HEAD"))
			.findFirst()
			.orElse(null);
	}

	private static boolean isShallowRepository(org.eclipse.jgit.lib.Repository repo) throws IOException {
		return !repo.getObjectDatabase().getShallowCommits().isEmpty();
	}

	private static void deleteRecursively(Path root) throws IOException {
		if (root == null || !Files.exists(root)) {
			return;
		}
		try (var paths = Files.walk(root)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					throw new RuntimeException("Failed to delete " + path, e);
				}
			});
		} catch (RuntimeException e) {
			if (e.getCause() instanceof IOException io) {
				throw io;
			}
			throw e;
		}
	}

	private static long getApiAnnotationsCount(API api, String fqn) {
		return api.getExportedTypes().stream()
			.mapToLong(type -> {
				long typeAnnotationCount = type.getAnnotations().stream()
					.filter(a -> a.actualAnnotation().getQualifiedName().equals(fqn))
					.count();
				long fieldsAnnotationCount = type.getDeclaredFields().stream()
					.filter(f -> f.getAnnotations().stream().anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(fqn)))
					.count();
				long methodsAnnotationCount = type.getDeclaredMethods().stream()
					.filter(m -> m.getAnnotations().stream().anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(fqn)))
					.count();
				return typeAnnotationCount + fieldsAnnotationCount + methodsAnnotationCount;
			})
			.sum();
	}

	private static long getInternalCount(API api, ExclusionMatcher exclusionMatcher) {
		return streamExportedSymbols(api)
			.filter(exclusionMatcher::isInternal)
			.count();
	}

	private static Stream<Symbol> streamExportedSymbols(API api) {
		return api.getExportedTypes().stream()
			.flatMap(type -> Stream.concat(
				Stream.of(type),
				Stream.concat(type.getDeclaredFields().stream(), type.getDeclaredMethods().stream())
			));
	}

	private static boolean hasAnnotation(Symbol symbol, String annotationFqn) {
		return symbol.getAnnotations().stream()
			.anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(annotationFqn));
	}

	private static boolean annotationMatches(Annotation annotation, RoseauOptions.AnnotationExclusion exclusion) {
		String actual = annotation.actualAnnotation().getQualifiedName();
		String expected = exclusion.name();
		if (expected.contains(".")) {
			return actual.equals(expected) && annotation.hasValues(exclusion.args());
		}
		String simpleName = actual.contains(".") ? actual.substring(actual.lastIndexOf('.') + 1) : actual;
		return simpleName.equals(expected) && annotation.hasValues(exclusion.args());
	}

	private static String compatibility(BreakingChangeKind kind) {
		if (kind.isBinaryBreaking() && kind.isSourceBreaking()) {
			return "both";
		}
		if (kind.isBinaryBreaking()) {
			return "binary";
		}
		return "source";
	}

	private static String visibility(AccessModifier visibility) {
		return visibility == null ? "" : visibility.toString();
	}

	private static void writeCsvRow(Writer writer, List<Object> values) throws IOException {
		String line = values.stream()
			.map(RepositoryWalkerUtils::csvCell)
			.collect(Collectors.joining(","));
		writer.write(line);
		writer.write(System.lineSeparator());
	}

	private static String csvCell(Object value) {
		String raw = value == null ? "" : String.valueOf(value);
		boolean needsQuoting = raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r");
		if (!needsQuoting) {
			return raw;
		}
		return "\"" + raw.replace("\"", "\"\"") + "\"";
	}

	private static Repository withDefaultExclusions(Repository repo) {
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

	private static RoseauOptions.Exclude toExclude(JsonNode node) {
		return MAPPER.convertValue(node, RoseauOptions.Exclude.class);
	}

	private static RoseauOptions.Exclude sanitizeExclude(RoseauOptions.Exclude exclude) {
		if (exclude == null) {
			return EMPTY_EXCLUDE;
		}
		List<String> names = exclude.names() == null ? List.of() : exclude.names();
		List<RoseauOptions.AnnotationExclusion> annotations = exclude.annotations() == null ? List.of() : exclude.annotations();
		return new RoseauOptions.Exclude(names, annotations);
	}

	private static Set<Path> relativizeUnderRoot(Set<Path> paths, Path sourceRootRelative) {
		return paths.stream()
			.filter(p -> p.startsWith(sourceRootRelative))
			.map(sourceRootRelative::relativize)
			.collect(Collectors.toSet());
	}

	private static void addIfJava(Set<Path> set, Path path) {
		if (isJavaPath(path)) {
			set.add(path);
		}
	}

	private static boolean isJavaPath(Path path) {
		return path != null && path.toString().endsWith(".java");
	}

	private static boolean isPomPath(Path path) {
		if (path == null) {
			return false;
		}
		String str = path.toString();
		return str.equals("pom.xml") || str.endsWith("/pom.xml");
	}

	private static Path pathOf(String path) {
		if (path == null || DiffEntry.DEV_NULL.equals(path)) {
			return null;
		}
		return Path.of(path);
	}
}
