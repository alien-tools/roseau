package io.github.alien.roseau.cli;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatter;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.diff.formatter.CsvFormatter;
import io.github.alien.roseau.extractors.ExtractorType;
import io.github.alien.roseau.extractors.MavenClasspathBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static picocli.CommandLine.ArgGroup;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

/**
 * Main class implementing a CLI for interacting with Roseau. See {@code --help} for usage information.
 */
@Command(name = "roseau", sortOptions = false,
	description = "Roseau detects breaking changes between two versions (--v1/--v2) of a module or library. " +
		"--v1 and --v2 can point to either JAR files or source code directories. " +
		"Example: roseau --diff --v1 /path/to/library-1.0.0.jar --v2 /path/to/library-2.0.0.jar")
public final class RoseauCLI implements Callable<Integer> {
	@Spec
	CommandSpec spec;
	@ArgGroup(exclusive = true, multiplicity = "1")
	RoseauCLI.Mode mode;
	private static class Mode {
		@Option(names = "--api", required = true,
			description = "Serialize the API model of --v1; see --json")
		boolean api;
		@Option(names = "--diff", required = true,
			description = "Compute breaking changes between versions --v1 and --v2")
		boolean diff;
	}
	@Option(names = "--v1", paramLabel = "<path>",
		description = "Path to the first version of the library; either a source directory or a JAR")
	private Path v1;
	@Option(names = "--v2", paramLabel = "<path>",
		description = "Path to the second version of the library; either a source directory or a JAR")
	private Path v2;
	@Option(names = "--extractor", paramLabel = "<extractor>",
		description = "API extractor to use: ${COMPLETION-CANDIDATES}")
	private ExtractorType extractorType;
	@Option(names = "--json", defaultValue = "api.json", paramLabel = "<path>",
		description = "Where to serialize the JSON API model of --v1; defaults to api.json")
	private Path apiPath;
	@Option(names = "--report", paramLabel = "<path>",
		description = "Where to write the breaking changes report")
	private Path reportPath;
	@Option(names = "--format",
		description = "Format of the report: ${COMPLETION-CANDIDATES}",
		defaultValue = "CSV")
	private BreakingChangesFormatterFactory format;
	@Option(names = "--pom", paramLabel = "<path>",
		description = "A pom.xml file to build a classpath from")
	private Path pom;
	@Option(names = "--classpath", split = ":", paramLabel = "<path>",
		description = "A colon-separated list of JARs to include in the classpath")
	private Set<Path> userClasspath = Set.of();
	@Option(names = "--ignored", paramLabel = "<path>",
		description = "Do not report the breaking changes listed in the given CSV file; " +
			"the CSV file share the same structure as the one produced by --format CSV (symbol;kind;nature)")
	private Path ignoredCsv;
	@Option(names = "--fail-on-bc",
		description = "Return a non-zero code if breaking changes are detected")
	private boolean failMode;
	@Option(names = "--plain",
		description = "Disable ANSI colors, output plain text")
	private boolean plain;
	@Option(names = "--verbose",
		description = "Print debug information")
	private boolean verbose;
	@CommandLine.Option(names = "--github-action", hidden = true)
	private boolean githubActionMode;

	private static final String RED_TEXT = "\u001B[31m";
	private static final String BOLD = "\u001B[1m";
	private static final String UNDERLINE = "\u001B[4m";
	private static final String RESET = "\u001B[0m";

	private API buildAPI(Library library) {
		Stopwatch sw = Stopwatch.createStarted();
		API api = Roseau.buildAPI(library);
		printVerbose("Extracting API from %s using %s took %dms (%d types)".formatted(
			library.getLocation(), library.getExtractorType(), sw.elapsed().toMillis(), api.getExportedTypes().size()));
		return api;
	}

	private RoseauReport diff(Library libraryV1, Library libraryV2) {
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(libraryV1));
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> buildAPI(libraryV2));

		API apiV1 = futureV1.join();
		API apiV2 = futureV2.join();

		Stopwatch sw = Stopwatch.createStarted();
		RoseauReport report = Roseau.diff(apiV1, apiV2);
		printVerbose("Diffing APIs took %dms (%d breaking changes)".formatted(
			sw.elapsed().toMillis(), report.breakingChanges().size()));

		return report;
	}

	private Set<Path> buildClasspath() {
		Set<Path> classpath = new HashSet<>(userClasspath);

		if (pom != null && Files.isRegularFile(pom)) {
			Stopwatch sw = Stopwatch.createStarted();
			MavenClasspathBuilder classpathBuilder = new MavenClasspathBuilder();
			classpath.addAll(classpathBuilder.buildClasspath(pom));
			printVerbose("Extracting classpath from %s took %dms".formatted(
				pom, sw.elapsed().toMillis()));
		}

		if (classpath.isEmpty()) {
			print("Warning: no classpath provided, results may be inaccurate");
		} else {
			printVerbose("Classpath: %s".formatted(classpath));
		}

		return classpath;
	}

	private void writeReport(API api, RoseauReport report) {
		if (githubActionMode) {
			writeGithubActionReport(api, report);
			return;
		}

		if (reportPath == null)
			return;
		}

		try {
			BreakingChangesFormatter fmt = BreakingChangesFormatterFactory.newBreakingChangesFormatter(format);
			Files.writeString(reportPath, fmt.format(api, report), StandardCharsets.UTF_8);
			print("Report has been written to %s".formatted(reportPath));
		} catch (IOException e) {
			printErr("Error writing report to %s: %s".formatted(reportPath, e.getMessage()));
		}
	}

	private void writeApiReport(API api) {
		if (apiPath == null) {
			return;
		}

		try {
			api.getLibraryTypes().writeJson(apiPath);
			print("API has been written to %s".formatted(apiPath));
		} catch (IOException e) {
			printErr("Error writing report to %s: %s".formatted(apiPath, e.getMessage()));
		}
	}

	private void writeGithubActionReport(API api, RoseauReport report) {
		try {
			Files.writeString(Path.of("report.csv"), new CsvFormatter().format(api, report));
			Files.writeString(Path.of("report.html"), new HtmlFormatter().format(api, report));
			Files.writeString(Path.of("report.md"), new MdFormatter().format(api, report));
			LOGGER.info("Wrote reports for github action");
		} catch (IOException e) {
			LOGGER.error("Couldn't write reports for github action", e);
		}
	}

	private String format(BreakingChange bc) {
		SourceLocation location = bc.impactedSymbol().getLocation() == SourceLocation.NO_LOCATION
			? bc.impactedType().getLocation()
			: bc.impactedSymbol().getLocation();
		boolean symbolInType = bc.impactedSymbol() instanceof TypeDecl ||
			bc.impactedSymbol() instanceof TypeMemberDecl member &&
				member.getContainingType().getQualifiedName().equals(bc.impactedType().getQualifiedName());

		if (plain) {
			return String.format("%s %s%s%n\t%s:%s", bc.kind(),
				bc.impactedSymbol().getQualifiedName(),
				symbolInType ? "" : " in " + bc.impactedType().getQualifiedName(),
				location.file(), location.line());
		} else {
			return String.format("%s %s%s%n\t%s:%s",
				RED_TEXT + BOLD + bc.kind() + RESET,
				UNDERLINE + bc.impactedSymbol().getQualifiedName() + RESET,
				symbolInType ? "" : " in " + bc.impactedType().getQualifiedName(),
				location.file(), location.line());
		}
	}

	private List<BreakingChange> filterIgnoredBCs(RoseauReport report) {
		if (ignoredCsv == null) {
			return report.breakingChanges();
		}

		try {
			record Ignored(String type, String symbol, BreakingChangeKind kind) {}
			List<String> lines = Files.readAllLines(ignoredCsv);
			List<Ignored> ignored = lines.stream()
				.filter(line -> !line.equals(CsvFormatter.HEADER))
				.map(line -> {
					String[] fields = line.split(";");
					if (fields.length < 3) {
						printErr("Malformed line %s ignored in %s".formatted(line, ignoredCsv));
						return null;
					} else {
						return new Ignored(fields[0], fields[1], BreakingChangeKind.valueOf(fields[2].toUpperCase(Locale.ROOT)));
					}
				})
				.filter(Objects::nonNull)
				.toList();

			return report.breakingChanges().stream()
				.filter(bc -> ignored.stream().noneMatch(ign ->
					bc.impactedType().getQualifiedName().equals(ign.type()) &&
					bc.impactedSymbol().getQualifiedName().equals(ign.symbol()) &&
					bc.kind() == ign.kind()))
				.toList();
		} catch (IOException e) {
			throw new RoseauException("Couldn't read CSV file %s".formatted(ignoredCsv), e);
		}
	}

	private void checkArguments() {
		if (v1 == null || !Files.exists(v1)) {
			throw new IllegalArgumentException("Cannot find v1: %s".formatted(v1));
		}

		if (mode.diff && (v2 == null || !Files.exists(v2))) {
			throw new IllegalArgumentException("Cannot find v2: %s".formatted(v2));
		}

		if (pom != null && !Files.isRegularFile(pom)) {
			throw new IllegalArgumentException("Cannot find pom: %s".formatted(pom));
		}

		if (ignoredCsv != null && !Files.isRegularFile(ignoredCsv)) {
			throw new IllegalArgumentException("Cannot find ignored CSV: %s".formatted(pom));
		}
	}

	@Override
	public Integer call() {
		try {
			if (verbose) {
				Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
			}

			checkArguments();
			List<Path> classpath = buildClasspath().stream().toList();
			Library.Builder builder1 = Library.builder()
				.location(v1)
				.classpath(classpath);
			if (extractorType != null) {
				builder1.extractorType(extractorType);
			}
			Library libraryV1 = builder1.build();

			if (mode.api) {
				API api = buildAPI(libraryV1);
				writeApiReport(api);
			}

			if (mode.diff) {
				Library.Builder builder2 = Library.builder()
					.location(v2)
					.classpath(classpath);
				if (extractorType != null) {
					builder2.extractorType(extractorType);
				}
				Library libraryV2 = builder2.build();
				RoseauReport report = diff(libraryV1, libraryV2);
				List<BreakingChange> bcs = filterIgnoredBCs(report);

				if (bcs.isEmpty()) {
					print("No breaking changes found.");
				} else {
					print(
						bcs.stream()
							.map(this::format)
							.collect(Collectors.joining(System.lineSeparator()))
					);
					writeReport(report.v1(), report);

					if (failMode) {
						return 1;
					}
				}
			}

			return 0;
		} catch (RuntimeException e) {
			if (verbose) {
				e.printStackTrace(spec.commandLine().getErr());
			} else {
				printErr(e.getMessage());
			}
			return 2;
		}
	}

	private void print(String message) {
		spec.commandLine().getOut().println(message);
	}

	private void printVerbose(String message) {
		if (verbose) {
			print(message);
		}
	}

	private void printErr(String message) {
		spec.commandLine().getErr().println(message);
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new RoseauCLI()).execute(args);
		System.exit(exitCode);
	}
}
