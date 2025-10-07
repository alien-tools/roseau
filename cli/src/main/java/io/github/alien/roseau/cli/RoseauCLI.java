package io.github.alien.roseau.cli;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.RoseauOptions;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
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
@Command(name = "roseau", version = "Roseau 0.4.0-SNAPSHOT", sortOptions = false, mixinStandardHelpOptions = true,
	description = "Roseau detects breaking changes between two versions (--v1/--v2) of a Java module or library. " +
		"--v1 and --v2 can point to either JAR files or source code directories. " +
		"Example: roseau --diff --v1 /path/to/library-1.0.0.jar --v2 /path/to/library-2.0.0.jar")
public final class RoseauCLI implements Callable<Integer> {
	@Spec
	CommandSpec spec;
	@ArgGroup(exclusive = true, multiplicity = "1")
	RoseauCLI.Mode mode;
	private static class Mode {
		@Option(names = "--api", required = true,
			description = "Serialize the API model of --v1; see --api-json")
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
	@Option(names = "--api-json", paramLabel = "<path>",
		description = "Where to serialize the JSON API model of --v1 in --api mode")
	private Path apiJson;
	@Option(names = "--report", paramLabel = "<path>",
		description = "Where to write the breaking changes report in --diff mode")
	private Path reportPath;
	@Option(names = "--format",
		description = "Format of the report: ${COMPLETION-CANDIDATES}")
	private BreakingChangesFormatterFactory format;
	@Option(names = "--classpath", paramLabel = "<path>[,<path>...]",
		description = "A colon-separated list of JARs to include in the classpath (Windows: semi-colon), " +
			"shared by --v1 and --v2")
	private String classpath;
	@Option(names = "--pom", paramLabel = "<path>",
		description = "A pom.xml file to build a classpath from, shared by --v1 and --v2")
	private Path pom;
	@Option(names = "--v1-classpath", paramLabel = "<path>[,<path>...]",
		description = "A --classpath for --v1")
	private String v1Classpath;
	@Option(names = "--v2-classpath", paramLabel = "<path>[,<path>...]",
		description = "A --classpath for --v2")
	private String v2Classpath;
	@Option(names = "--v1-pom", paramLabel = "<path>",
		description = "A --pom for --v1")
	private Path v1Pom;
	@Option(names = "--v2-pom", paramLabel = "<path>",
		description = "A --pom for --v2")
	private Path v2Pom;
	@Option(names = "--v1-extractor", paramLabel = "<extractor>",
		description = "An --extractor for --v1")
	private ExtractorType v1ExtractorType;
	@Option(names = "--v2-extractor", paramLabel = "<extractor>",
		description = "An --extractor for --v2")
	private ExtractorType v2ExtractorType;
	@Option(names = "--ignored", paramLabel = "<path>",
		description = "Do not report the breaking changes listed in the given CSV file; " +
			"this CSV file shares the same structure as the one produced by --format CSV")
	private Path ignoredCsv;
	@Option(names = "--config", paramLabel = "<path>",
		description = "A roseau.yaml config file; overridden by CLI options")
	private Path config;
	@Option(names = "--fail-on-bc",
		description = "Return 1 if breaking changes are detected")
	private boolean failMode;
	@Option(names = "--plain",
		description = "Disable ANSI colors, output plain text")
	private boolean plain;
	@Option(names = {"-v", "--verbose"},
		description = "Increase verbosity (-v, -vv).")
	private boolean[] verbosity;
	private boolean verbose;
	private boolean debug;

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

	private static Set<Path> buildClasspathFromString(String cp) {
		return cp != null
			? Arrays.stream(cp.split(File.pathSeparator))
			.filter(p -> p.endsWith(".jar"))
			.map(Path::of)
			.collect(Collectors.toSet())
			: Set.of();
	}

	private void writeReport(RoseauReport report, BreakingChangesFormatterFactory format, Path path) {
		try {
			Files.createDirectories(path.getParent());
			BreakingChangesFormatter fmt = BreakingChangesFormatterFactory.newBreakingChangesFormatter(format);
			Files.writeString(path, fmt.format(report), StandardCharsets.UTF_8);
			printVerbose("Report has been written to %s".formatted(path));
		} catch (IOException e) {
			throw new RoseauException("Error writing report to %s".formatted(path), e);
		}
	}

	private void writeApiReport(API api, Path apiPath) {
		try {
			Files.createDirectories(apiPath.getParent());
			api.getLibraryTypes().writeJson(apiPath);
			printVerbose("API has been written to %s".formatted(apiPath));
		} catch (IOException e) {
			throw new RoseauException("Error writing API to %s".formatted(apiPath), e);
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

	private List<BreakingChange> filterIgnoredBCs(RoseauReport report, Path ignoredPath) {
		try {
			record Ignored(String type, String symbol, BreakingChangeKind kind) {}
			List<String> lines = Files.readAllLines(ignoredPath);
			List<Ignored> ignored = lines.stream()
				.filter(line -> !line.equals(CsvFormatter.HEADER))
				.map(line -> {
					String[] fields = line.split(";");
					if (fields.length < 3 ||
						Arrays.stream(BreakingChangeKind.values()).map(Enum::name).noneMatch(name -> name.equals(fields[2]))) {
						printErr("Malformed line %s ignored in %s".formatted(line, ignoredPath));
						return null;
					} else {
						return new Ignored(fields[0], fields[1], BreakingChangeKind.valueOf(fields[2]));
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
			throw new RoseauException("Couldn't read CSV file %s".formatted(ignoredPath), e);
		}
	}

	private void checkOptions(RoseauOptions options) {
		Path v1Path = options.v1().location();
		if (v1Path == null || !Files.exists(v1Path)) {
			throw new RoseauException("Cannot find v1: %s".formatted(v1Path));
		}

		if (mode.api && options.v1().apiReport() == null) {
			throw new RoseauException("Path to a JSON file required in --api mode");
		}

		Path v2Path = options.v2().location();
		if (mode.diff && (v2Path == null || !Files.exists(v2Path))) {
			throw new RoseauException("Cannot find v2: %s".formatted(v2Path));
		}

		if (reportPath != null && format == null) {
			throw new RoseauException("--format required with --report");
		}

		Path v1PomPath = options.v1().classpath().pom();
		if (v1PomPath != null && !Files.isRegularFile(v1PomPath)) {
			throw new RoseauException("Cannot find pom: %s".formatted(v1PomPath));
		}

		Path v2PomPath = options.v2().classpath().pom();
		if (v2PomPath != null && !Files.isRegularFile(v2PomPath)) {
			throw new RoseauException("Cannot find pom: %s".formatted(v2PomPath));
		}

		Path ignoredPath = options.ignore();
		if (ignoredPath != null && !Files.isRegularFile(ignoredPath)) {
			throw new RoseauException("Cannot find ignored CSV: %s".formatted(ignoredPath));
		}
	}

	private RoseauOptions makeCliOptions() {
		// No CLI option (yet?) for API exclusions
		RoseauOptions.Exclude noExclusions = new RoseauOptions.Exclude(List.of(), List.of());
		RoseauOptions.Common commonCli = new RoseauOptions.Common(
			extractorType,
			new RoseauOptions.Classpath(pom, buildClasspathFromString(classpath)),
			noExclusions
		);
		RoseauOptions.Library v1Cli = new RoseauOptions.Library(
			v1, v1ExtractorType, new RoseauOptions.Classpath(v1Pom, buildClasspathFromString(v1Classpath)),
			noExclusions, null
		);
		RoseauOptions.Library v2Cli = new RoseauOptions.Library(
			v2, v2ExtractorType, new RoseauOptions.Classpath(v2Pom, buildClasspathFromString(v2Classpath)),
			noExclusions, null
		);
		List<RoseauOptions.Report> reportsCli = (reportPath != null && format != null)
			? List.of(new RoseauOptions.Report(reportPath, format))
			: List.of();
		return new RoseauOptions(commonCli, v1Cli, v2Cli, ignoredCsv, reportsCli);
	}

	private void doApi(Library library, RoseauOptions.Library libraryOptions) {
		if (library.getClasspath().isEmpty()) {
			printErr("Warning: no classpath provided for %s, results may be inaccurate".formatted(library.getLocation()));
		}
		API api = buildAPI(library);
		if (libraryOptions.apiReport() != null) {
			writeApiReport(api, libraryOptions.apiReport());
		}
	}

	private boolean doDiff(Library v1, Library v2, RoseauOptions options) {
		if (v1.getClasspath().isEmpty()) {
			printErr("Warning: no classpath provided for %s, results may be inaccurate".formatted(v1.getLocation()));
		}
		if (v2.getClasspath().isEmpty()) {
			printErr("Warning: no classpath provided for %s, results may be inaccurate".formatted(v2.getLocation()));
		}
		RoseauReport report = diff(v1, v2);
		Path ignoreFile = options.ignore();
		List<BreakingChange> bcs = ignoreFile != null && Files.isRegularFile(ignoreFile)
			? filterIgnoredBCs(report, ignoreFile)
			: report.breakingChanges();

		if (bcs.isEmpty()) {
			print("No breaking changes found.");
		} else {
			print(
				bcs.stream()
					.map(this::format)
					.collect(Collectors.joining(System.lineSeparator()))
			);
		}

		if (options.v1().apiReport() != null) {
			writeApiReport(report.v1(), options.v1().apiReport());
		}
		if (options.v2().apiReport() != null) {
			writeApiReport(report.v2(), options.v2().apiReport());
		}
		options.reports().forEach(reportOption ->
			writeReport(report, reportOption.format(), reportOption.file())
		);

		return !bcs.isEmpty();
	}

	@Override
	public Integer call() {
		try {
			if (verbosity != null) {
				this.verbose = verbosity.length > 0;
				this.debug = verbosity.length > 1;
			}

			if (debug) {
				Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
			} else if (verbose) {
				Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);
			}

			RoseauOptions cliOptions = makeCliOptions();
			RoseauOptions fileOptions = config != null && Files.isRegularFile(config)
				? RoseauOptions.load(config)
				: RoseauOptions.newDefault();
			RoseauOptions options = fileOptions.mergeWith(cliOptions);
			checkOptions(options);
			printDebug("Options are " + options);

			if (mode.api) {
				Library libraryV1 = options.v1().mergeWith(options.common()).toLibrary();
				printDebug("v1 = " + libraryV1);
				doApi(libraryV1, options.v1());
			}

			if (mode.diff) {
				Library libraryV1 = options.v1().mergeWith(options.common()).toLibrary();
				Library libraryV2 = options.v2().mergeWith(options.common()).toLibrary();
				printDebug("v1 = " + libraryV1);
				printDebug("v2 = " + libraryV2);
				boolean breaking = doDiff(libraryV1, libraryV2, options);

				if (breaking && failMode) {
					return 1;
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

	private void printDebug(String message) {
		if (debug) {
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
