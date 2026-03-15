package io.github.alien.roseau.git;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.Symbol;
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
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Writes {@link CommitAnalysis} results to two CSV files: one for commit-level data
 * and one for individual breaking changes.
 */
final class CsvReporter implements GitWalker.CommitSink, AutoCloseable {
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
		"version",
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
		"deprecated_count",
		"internal_count",
		"breaking_changes_count",
		"binary_breaking_changes_count",
		"source_breaking_changes_count",
		"api_changed",
		"has_java_changes",
		"has_pom_changes",
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

	// --- Instance state ---

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

		Path parent = outputDir == null ? Path.of(".") : outputDir;
		Files.createDirectories(parent);

		Path commitsCsv = parent.resolve(config.libraryId() + "-commits.csv");
		Path bcsCsv = parent.resolve(config.libraryId() + "-bcs.csv");
		LOGGER.info("Writing commit data to {}", commitsCsv.toAbsolutePath().normalize());
		LOGGER.info("Writing breaking changes data to {}", bcsCsv.toAbsolutePath().normalize());

		this.commitsWriter = openWriter(commitsCsv);
		this.bcsWriter = openWriter(bcsCsv);
		writeCsvHeader(commitsWriter, COMMITS_HEADER);
		writeCsvHeader(bcsWriter, BCS_HEADER);
	}

	@Override
	public void accept(CommitAnalysis analysis) throws IOException {
		StatsResult stats = resolveApiStats(analysis);
		long daysSincePrev = daysSincePreviousCommit(previousCommitTime, analysis.commit().commitTime());
		writeCommitRow(analysis, stats, daysSincePrev);
		if (analysis.report().isPresent()) {
			writeBreakingChangesRows(analysis.commit().sha(), analysis.report().get());
		}
		previousCommitTime = analysis.commit().commitTime();
	}

	@Override
	public void close() throws IOException {
		commitsWriter.close();
		bcsWriter.close();
	}

	// --- API stats ---

	private record ApiStats(
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

	private static final ApiStats EMPTY_STATS = new ApiStats(0, 0, 0, 0, 0, 0, 0, 0);

	private record StatsResult(ApiStats stats, long timeMs) {
	}

	private StatsResult resolveApiStats(CommitAnalysis analysis) {
		if (analysis.api().isEmpty()) {
			return new StatsResult(EMPTY_STATS, 0);
		}
		API api = analysis.api().get();
		if (api == cachedApi && cachedStats != null) {
			return new StatsResult(cachedStats, 0);
		}
		Stopwatch sw = Stopwatch.createStarted();
		cachedStats = computeApiStats(api);
		cachedApi = api;
		return new StatsResult(cachedStats, sw.elapsed().toMillis());
	}

	private ApiStats computeApiStats(API api) {
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
			countAnnotated(api, "java.lang.Deprecated"),
			countInternal(api)
		);
	}

	private long countAnnotated(API api, String fqn) {
		return api.getExportedTypes().stream()
			.mapToLong(type -> {
				long typeCount = type.getAnnotations().stream()
					.filter(a -> a.actualAnnotation().getQualifiedName().equals(fqn))
					.count();
				long fieldCount = type.getDeclaredFields().stream()
					.filter(f -> f.getAnnotations().stream().anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(fqn)))
					.count();
				long methodCount = type.getDeclaredMethods().stream()
					.filter(m -> m.getAnnotations().stream().anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(fqn)))
					.count();
				return typeCount + fieldCount + methodCount;
			})
			.sum();
	}

	private long countInternal(API api) {
		return api.getExportedTypes().stream()
			.flatMap(type -> Stream.concat(
				Stream.of(type),
				Stream.concat(type.getDeclaredFields().stream(), type.getDeclaredMethods().stream())))
			.filter(exclusionMatcher::isInternal)
			.count();
	}

	// --- Exclusion matching ---

	private record ExclusionMatcher(List<Pattern> namePatterns, List<RoseauOptions.AnnotationExclusion> annotationExclusions) {
		static ExclusionMatcher of(RoseauOptions.Exclude exclusions) {
			List<Pattern> patterns = exclusions.names().stream()
				.map(Pattern::compile)
				.toList();
			return new ExclusionMatcher(patterns, exclusions.annotations());
		}

		boolean isInternal(Symbol symbol) {
			if (symbol == null) {
				return false;
			}
			if (namePatterns.stream().anyMatch(p -> p.matcher(symbol.getQualifiedName()).matches())) {
				return true;
			}
			return annotationExclusions.stream()
				.anyMatch(excl -> symbol.getAnnotations().stream().anyMatch(ann -> annotationMatches(ann, excl)));
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

	// --- Commit row ---

	private void writeCommitRow(CommitAnalysis analysis, StatsResult stats, long daysSincePrev) throws IOException {
		CommitInfo c = analysis.commit();
		String tags = String.join(";", c.tags());
		String error = analysis.errors().stream()
			.map(Exception::getMessage)
			.collect(Collectors.joining("; "));
		int bcCount = analysis.report().map(r -> r.getAllBreakingChanges().size()).orElse(0);
		int binaryBcCount = analysis.report()
			.map(r -> (int) r.getAllBreakingChanges().stream().map(BreakingChange::kind).filter(BreakingChangeKind::isBinaryBreaking).count())
			.orElse(0);
		int sourceBcCount = analysis.report()
			.map(r -> (int) r.getAllBreakingChanges().stream().map(BreakingChange::kind).filter(BreakingChangeKind::isSourceBreaking).count())
			.orElse(0);

		writeCsvRow(commitsWriter, List.of(
			libraryId,
			c.sha(),
			RepositoryWalkerUtils.commitUrl(url, c.sha()),
			c.shortMessage(),
			c.conventionalCommitTag(),
			c.isConventionalBreakingChange(),
			c.parentSha(),
			c.commitTime().toString(),
			c.isMergeCommit(),
			c.branch(),
			tags,
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
			stats.stats().deprecatedCount(),
			stats.stats().internalCount(),
			bcCount,
			binaryBcCount,
			sourceBcCount,
			analysis.apiChanged(),
			c.javaChanged(),
			c.pomChanged(),
			analysis.checkoutTimeMs(),
			0L,
			analysis.apiTimeMs(),
			analysis.diffTimeMs(),
			stats.timeMs(),
			error
		));
	}

	// --- Breaking changes rows ---

	private void writeBreakingChangesRows(String commitSha, RoseauReport report) throws IOException {
		for (BreakingChange bc : report.getAllBreakingChanges()) {
			SourceLocation location = bc.getLocation();
			Symbol impactedSymbol = bc.impactedSymbol();
			BreakingChangeKind kind = bc.kind();
			boolean isExcludedSymbol = report.isExcluded(impactedSymbol)
				|| report.isExcluded(bc.impactedType())
				|| exclusionMatcher.isInternal(impactedSymbol)
				|| exclusionMatcher.isInternal(bc.impactedType());
			boolean isRemoval = kind.getNature() == BreakingChangeNature.DELETION;
			boolean isDeprecatedRemoval = isRemoval && hasAnnotation(impactedSymbol, "java.lang.Deprecated");
			boolean isInternalRemoval = isRemoval && exclusionMatcher.isInternal(impactedSymbol);
			writeCsvRow(bcsWriter, List.of(
				libraryId,
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

	// --- CSV utilities ---

	private static BufferedWriter openWriter(Path file) throws IOException {
		return Files.newBufferedWriter(file,
			StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private static void writeCsvHeader(Writer writer, List<String> header) throws IOException {
		writeCsvRow(writer, header);
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

	private static long daysSincePreviousCommit(Instant previous, Instant current) {
		if (previous == null) {
			return 0;
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
