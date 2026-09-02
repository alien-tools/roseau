package io.github.alien.roseau.git;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.changes.BreakingChangeNature;
import io.github.alien.roseau.options.RoseauOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Writes {@link CommitAnalysis} results to two CSV files: one for commit-level data
 * and one for individual breaking changes.
 */
final class CsvReporter implements CommitSink, AutoCloseable {
	private static final Logger LOGGER = LogManager.getLogger(CsvReporter.class);

	private static final List<String> COMMITS_HEADER = List.of(
		"library",
		"commit_sha",
		"commit_url",
		"commit_short_msg",
		"conventional_commit_tag",
		"is_conventional_breaking",
		"parent_commit",
		"date_utc",
		"is_merge_commit",
		"branch",
		"tag",
		"days_since_prev_commit",
		"files_changed",
		"loc_added",
		"loc_deleted",
		"updated_java_files_count",
		"deleted_java_files_count",
		"created_java_files_count",
		"all_api_types_count",
		"all_api_methods_count",
		"all_api_fields_count",
		"all_api_symbols_count",
		"exported_types_count",
		"exported_methods_count",
		"exported_fields_count",
		"exported_symbols_count",
		"deprecated_count",
		"internal_count",
		"all_breaking_changes_count",
		"api_breaking_changes_count",
		"excluded_breaking_changes_count",
		"binary_breaking_changes_count",
		"source_breaking_changes_count",
		"api_changed",
		"has_java_changes",
		"has_pom_changes",
		"source_root",
		"unresolved_types_count",
		"incomplete_hierarchy_types_count",
		"checkout_time_ms",
		"classpath_time_ms",
		"api_time_ms",
		"diff_time_ms",
		"stats_time_ms",
		"error"
	);

	private static final List<String> BCS_HEADER = List.of(
		"library",
		"commit",
		"commit_url",
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
		"matched_exclusion_rule",
		"impacted_type_hierarchy_incomplete",
		"source_file",
		"source_line"
	);

	private final String libraryId;
	private final String url;
	private final BufferedWriter commitsWriter;
	private final BufferedWriter bcsWriter;
	private final ExclusionMatcher exclusionMatcher;

	private Instant previousCommitTime;
	private API cachedApi;
	private ApiStats cachedStats;

	CsvReporter(GitWalker.Config config, Path outputDir) throws IOException {
		this.libraryId = config.libraryId();
		this.url = config.url();
		this.exclusionMatcher = ExclusionMatcher.of(config.exclusions());

		Files.createDirectories(outputDir);

		Path commitsCsv = outputDir.resolve(config.libraryId() + "-commits.csv");
		Path bcsCsv = outputDir.resolve(config.libraryId() + "-bcs.csv");
		LOGGER.info("Writing commit data to {}", commitsCsv.toAbsolutePath().normalize());
		LOGGER.info("Writing breaking changes data to {}", bcsCsv.toAbsolutePath().normalize());

		this.commitsWriter = openWriter(commitsCsv);
		this.bcsWriter = openWriter(bcsCsv);
		writeCsvRow(commitsWriter, COMMITS_HEADER);
		writeCsvRow(bcsWriter, BCS_HEADER);
	}

	@Override
	public void accept(CommitAnalysis analysis) {
		StatsResult stats = computeApiStats(analysis);
		long daysSincePrev = daysBetweenInstants(previousCommitTime, analysis.commit().commitTime());

		List<BreakingChange> bcs = analysis.report().map(RoseauReport::getAllBreakingChanges).orElse(List.of());
		// One exclusion decision per breaking change, shared by the commit row and the breaking-change rows
		Map<BreakingChange, String> matchedRules = analysis.api()
			.map(api -> matchExclusions(bcs, api))
			.orElseGet(Map::of);

		try {
			writeCommitRow(analysis, stats, daysSincePrev, bcs, matchedRules);
			if (analysis.api().isPresent()) {
				writeBreakingChangesRows(analysis.commit().sha(), bcs, analysis.api().get(), matchedRules);
			}
		} catch (IOException e) {
			// A failed write costs one row, not the whole history: log it and keep walking
			LOGGER.error("Error writing data for commit {}", analysis.commit().sha(), e);
		} finally {
			// Advance regardless, so a failed write does not make the next commit's delta span two commits
			previousCommitTime = analysis.commit().commitTime();
		}
	}

	@Override
	public void close() throws IOException {
		commitsWriter.close();
		bcsWriter.close();
	}

	private record ApiStats(
		int allApiTypesCount,
		int allApiMethodsCount,
		int allApiFieldsCount,
		int exportedTypesCount,
		int exportedMethodsCount,
		int exportedFieldsCount,
		long deprecatedCount,
		long internalCount,
		int unresolvedTypesCount,
		int incompleteHierarchyTypesCount
	) {
		int allApiSymbolsCount() {
			return allApiTypesCount + allApiMethodsCount + allApiFieldsCount;
		}

		int exportedSymbolsCount() {
			return exportedTypesCount + exportedMethodsCount + exportedFieldsCount;
		}
	}

	private static final ApiStats EMPTY_STATS = new ApiStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

	private record StatsResult(ApiStats stats, long timeMs) {
	}

	private StatsResult computeApiStats(CommitAnalysis analysis) {
		if (analysis.api().isEmpty()) {
			return new StatsResult(EMPTY_STATS, 0L);
		}
		API api = analysis.api().get();
		if (api == cachedApi && cachedStats != null) {
			return new StatsResult(cachedStats, 0L);
		}
		Stopwatch sw = Stopwatch.createStarted();
		cachedStats = computeApiStats(api);
		cachedApi = api;
		return new StatsResult(cachedStats, sw.elapsed().toMillis());
	}

	private ApiStats computeApiStats(API api) {
		Collection<TypeDecl> allTypes = api.getLibraryTypes().getAllTypes();
		int allMethodsCount = 0;
		int allFieldsCount = 0;
		for (TypeDecl type : allTypes) {
			allMethodsCount += type.getDeclaredMethods().size();
			allFieldsCount += type.getDeclaredFields().size();
		}

		// Single pass over the exported types: the exported member sets are expensive to compute, so they are
		// materialised once per type and every counter is derived from them. Resolution is demand-driven, so walking
		// each hierarchy here is also what makes the unresolved-reference counters deterministic for every commit.
		String deprecated = Deprecated.class.getCanonicalName();
		int exportedTypesCount = 0;
		int exportedMethodsCount = 0;
		int exportedFieldsCount = 0;
		int incompleteHierarchyTypesCount = 0;
		long deprecatedCount = 0;
		long internalCount = 0;
		for (TypeDecl type : api.getExportedTypes()) {
			Set<MethodDecl> methods = api.analyzer().getDeclaredExportedMethods(type);
			Set<FieldDecl> fields = api.analyzer().getDeclaredExportedFields(type);

			exportedTypesCount++;
			exportedMethodsCount += methods.size();
			exportedFieldsCount += fields.size();
			if (api.analyzer().hasIncompleteHierarchy(type)) {
				incompleteHierarchyTypesCount++;
			}
			if (hasAnnotation(type, deprecated)) {
				deprecatedCount++;
			}
			if (exclusionMatcher.isInternal(type, api)) {
				internalCount++;
			}
			for (TypeMemberDecl member : Iterables.concat(methods, fields)) {
				if (hasAnnotation(member, deprecated)) {
					deprecatedCount++;
				}
				if (exclusionMatcher.isInternal(member, api)) {
					internalCount++;
				}
			}
		}

		return new ApiStats(
			allTypes.size(),
			allMethodsCount,
			allFieldsCount,
			exportedTypesCount,
			exportedMethodsCount,
			exportedFieldsCount,
			deprecatedCount,
			internalCount,
			api.analyzer().resolver().unresolvedTypes().size(),
			incompleteHierarchyTypesCount
		);
	}

	private record ExclusionMatcher(List<Pattern> namePatterns,
	                                List<RoseauOptions.AnnotationExclusion> annotationExclusions) {
		static ExclusionMatcher of(RoseauOptions.Exclude exclusions) {
			List<Pattern> patterns = exclusions.names().stream()
				.map(Pattern::compile)
				.toList();
			return new ExclusionMatcher(patterns, exclusions.annotations());
		}

		boolean isInternal(Symbol symbol, API api) {
			return matchedRule(symbol, api).isPresent();
		}

		Optional<String> matchedRule(Symbol symbol, API api) {
			if (symbol == null) {
				return Optional.empty();
			}
			Optional<String> byName = namePatterns.stream()
				.filter(p -> p.matcher(symbol.getQualifiedName()).matches())
				.findFirst()
				.map(p -> "name:" + p.pattern());
			if (byName.isPresent()) {
				return byName;
			}
			return switch (symbol) {
				case TypeDecl type -> matchedAnnotation(type)
					.or(() -> type.getEnclosingType()
						.flatMap(api.analyzer().resolver()::resolve)
						.flatMap(enclosing -> matchedRule(enclosing, api)));
				case TypeMemberDecl member -> api.analyzer().resolver().resolve(member.getContainingType())
					.flatMap(type -> matchedRule(type, api))
					.or(() -> matchedAnnotation(member));
			};
		}

		private Optional<String> matchedAnnotation(Symbol symbol) {
			return annotationExclusions.stream()
				.filter(excl -> symbol.getAnnotations().stream().anyMatch(ann -> annotationMatches(ann, excl)))
				.findFirst()
				.map(excl -> "annotation:" + excl.name());
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
	}

	/**
	 * Maps every excluded breaking change to the configured rule that excluded it. Breaking changes are compared by
	 * identity: they are deeply-structured records, so hashing them would be needlessly expensive.
	 */
	private Map<BreakingChange, String> matchExclusions(List<BreakingChange> bcs, API api) {
		Map<BreakingChange, String> matchedRules = new IdentityHashMap<>();
		for (BreakingChange bc : bcs) {
			exclusionMatcher.matchedRule(bc.impactedSymbol(), api)
				.or(() -> exclusionMatcher.matchedRule(bc.impactedType(), api))
				.ifPresent(rule -> matchedRules.put(bc, rule));
		}
		return matchedRules;
	}

	private void writeCommitRow(CommitAnalysis analysis, StatsResult stats, long daysSincePrev,
	                            List<BreakingChange> bcs, Map<BreakingChange, String> matchedRules) throws IOException {
		CommitInfo c = analysis.commit();
		String tags = String.join(";", c.tags());
		String error = String.join("; ", analysis.errors());
		int allBcCount = bcs.size();
		long binaryBcCount = bcs.stream().filter(bc -> bc.kind().isBinaryBreaking()).count();
		long sourceBcCount = bcs.stream().filter(bc -> bc.kind().isSourceBreaking()).count();
		long excludedBcCount = matchedRules.size();
		long apiBcCount = allBcCount - excludedBcCount;

		writeCsvRow(commitsWriter, List.of(
			libraryId,
			c.sha(),
			commitUrl(url, c.sha()),
			c.shortMessage(),
			c.conventionalCommitTag(),
			c.isConventionalBreakingChange(),
			c.parentSha(),
			c.commitTime().toString(),
			c.isMergeCommit(),
			c.branch(),
			tags,
			daysSincePrev,
			c.filesChanged(),
			c.locAdded(),
			c.locDeleted(),
			c.updatedJavaFiles().size(),
			c.deletedJavaFiles().size(),
			c.createdJavaFiles().size(),
			stats.stats().allApiTypesCount(),
			stats.stats().allApiMethodsCount(),
			stats.stats().allApiFieldsCount(),
			stats.stats().allApiSymbolsCount(),
			stats.stats().exportedTypesCount(),
			stats.stats().exportedMethodsCount(),
			stats.stats().exportedFieldsCount(),
			stats.stats().exportedSymbolsCount(),
			stats.stats().deprecatedCount(),
			stats.stats().internalCount(),
			allBcCount,
			apiBcCount,
			excludedBcCount,
			binaryBcCount,
			sourceBcCount,
			analysis.apiChanged(),
			c.javaChanged(),
			c.pomChanged(),
			analysis.sourceRoot().map(Path::toString).orElse(""),
			stats.stats().unresolvedTypesCount(),
			stats.stats().incompleteHierarchyTypesCount(),
			analysis.checkoutTimeMs(),
			0L,
			analysis.apiTimeMs(),
			analysis.diffTimeMs(),
			stats.timeMs(),
			error
		));
	}

	private String commitUrl(String url, String sha) {
		return url + "/commit/" + sha;
	}

	private void writeBreakingChangesRows(String commitSha, List<BreakingChange> bcs, API api,
	                                      Map<BreakingChange, String> matchedRules) throws IOException {
		for (BreakingChange bc : bcs) {
			SourceLocation location = bc.getLocation();
			Symbol impactedSymbol = bc.impactedSymbol();
			BreakingChangeKind kind = bc.kind();
			String matchedRule = matchedRules.getOrDefault(bc, "");
			boolean isExcludedSymbol = !matchedRule.isEmpty();
			boolean hierarchyIncomplete = api.analyzer().hasIncompleteHierarchy(bc.impactedType());
			boolean isRemoval = kind.getNature() == BreakingChangeNature.DELETION;
			boolean isDeprecatedRemoval = isRemoval && hasAnnotation(impactedSymbol, Deprecated.class.getCanonicalName());
			boolean isInternalRemoval = isRemoval && isExcludedSymbol;
			writeCsvRow(bcsWriter, List.of(
				libraryId,
				commitSha,
				commitUrl(url, commitSha),
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
				matchedRule,
				hierarchyIncomplete,
				location.file() != null ? location.file().toString() : "",
				location.line() >= 0 ? location.line() : ""
			));
		}
	}

	private static BufferedWriter openWriter(Path file) throws IOException {
		return Files.newBufferedWriter(file,
			StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private static void writeCsvRow(Writer writer, List<?> values) throws IOException {
		String line = values.stream()
			.map(CsvReporter::csvCell)
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

	private static long daysBetweenInstants(Instant previous, Instant current) {
		if (previous == null) {
			return 0L;
		}
		return ChronoUnit.DAYS.between(previous, current);
	}

	private static boolean hasAnnotation(Symbol symbol, String annotationFqn) {
		return symbol.getAnnotations().stream()
			.anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(annotationFqn));
	}

	private static String compatibility(BreakingChangeKind kind) {
		if (kind.isBinaryBreaking() && kind.isSourceBreaking()) {
			return "both";
		}
		return kind.isBinaryBreaking() ? "binary" : "source";
	}

	private static String visibility(AccessModifier visibility) {
		return visibility == null ? "" : visibility.toString();
	}
}
