package io.github.alien.roseau.cli;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.RoseauOptions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatter;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.diff.formatter.CliFormatter;
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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static picocli.CommandLine.ArgGroup;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

/**
 * Main class implementing a CLI for interacting with Roseau. See {@code --help} for usage information.
 */
@Command(name = "roseau", sortOptions = false, mixinStandardHelpOptions = true,
	versionProvider = RoseauCLI.VersionProvider.class,
	description = "Roseau detects breaking changes between two versions (--v1/--v2) of a Java module or library. " +
		"--v1 and --v2 can point to either JAR files or source code directories. " +
		"Example: roseau --diff --v1 /path/to/library-1.0.0.jar --v2 /path/to/library-2.0.0.jar")
public final class RoseauCLI implements Callable<Integer> {
	private Console console;
	@Spec
	private CommandSpec spec;
	@ArgGroup(exclusive = true, multiplicity = "1")
	private Mode mode;

	private static class Mode {
		@Option(names = "--api",
			description = "Serialize the API model of --v1; see --api-json")
		boolean api;
		@Option(names = "--diff",
			description = "Compute breaking changes between versions --v1 and --v2")
		boolean diff;
	}

	@Option(names = "--v1", paramLabel = "<path>",
		description = "Path to the first version of the library; either a source directory or a JAR")
	private Path v1;
	@Option(names = "--v2", paramLabel = "<path>",
		description = "Path to the second version of the library; either a source directory or a JAR")
	private Path v2;
	@Option(names = "--api-json", paramLabel = "<path>",
		description = "Where to serialize the Json API model of --v1 in --api mode")
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
		description = "A pom.xml file to extract the classpath from, shared by --v1 and --v2")
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
	@Option(names = "--binary-only",
		description = "Only report binary-breaking changes")
	private Boolean binaryOnly;
	@Option(names = "--source-only",
		description = "Only report source-breaking changes")
	private Boolean sourceOnly;
	@Option(names = "--ignored", paramLabel = "<path>",
		description = "Do not report the breaking changes listed in the given CSV file; " +
			"this CSV file shares the same structure as the one produced by --format CSV")
	private Path ignoredCsv;
	@Option(names = "--config", paramLabel = "<path>",
		description = "A roseau.yaml config file; CLI options take precedence over these options")
	private Path config;
	@Option(names = "--fail-on-bc",
		description = "Return with exit code 1 if breaking changes are detected")
	private boolean failMode;
	@Option(names = "--plain",
		description = "Disable ANSI colors, output plain text")
	private boolean plain;
	@Option(names = {"-v", "--verbose"},
		description = "Increase verbosity (-v, -vv).")
	private boolean[] verbosityLevel;

	private RoseauReport diff(Library libraryV1, Library libraryV2) {
		Stopwatch sw = Stopwatch.createStarted();

		console.printVerbose("Building APIs...  ");
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> Roseau.buildAPI(libraryV1));
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> Roseau.buildAPI(libraryV2));
		API apiV1 = futureV1.join();
		API apiV2 = futureV2.join();
		console.printlnVerbose("%d types → %d types (%d ms)".formatted(apiV1.getLibraryTypes().getAllTypes().size(),
			apiV2.getLibraryTypes().getAllTypes().size(), sw.elapsed().toMillis()));

		sw.reset().start();
		console.printVerbose("Comparing APIs... ");
		RoseauReport report = Roseau.diff(apiV1, apiV2);
		console.printlnVerbose("%d breaking changes (%d ms)".formatted(report.getBreakingChanges().size(),
			sw.elapsed().toMillis()));

		return report;
	}

	private static List<Path> buildClasspathFromString(String cp) {
		if (cp == null) {
			return List.of();
		}

		return Arrays.stream(cp.split(File.pathSeparator))
			.filter(p -> p.endsWith(".jar"))
			.map(Path::of)
			.toList();
	}

	private void writeReport(RoseauReport report, BreakingChangesFormatterFactory format, Path path) {
		try {
			if (path.getParent() != null) {
				Files.createDirectories(path.getParent());
			}
			BreakingChangesFormatter fmt = BreakingChangesFormatterFactory.newBreakingChangesFormatter(format);
			Files.writeString(path, fmt.format(report), StandardCharsets.UTF_8);
			console.printlnVerbose("Report has been written to %s".formatted(path));
		} catch (IOException e) {
			throw new RoseauException("Error writing report to %s".formatted(path), e);
		}
	}

	private void writeApiReport(API api, Path apiPath) {
		try {
			if (apiPath.getParent() != null) {
				Files.createDirectories(apiPath.getParent());
			}
			api.getLibraryTypes().writeJson(apiPath);
			console.printlnVerbose("API has been written to %s".formatted(apiPath));
		} catch (IOException e) {
			throw new RoseauException("Error writing API to %s".formatted(apiPath), e);
		}
	}

	private RoseauReport filterReport(RoseauReport report, RoseauOptions.Diff diffOptions) {
		List<BreakingChange> bcs = diffOptions.sourceOnly()
			? report.getSourceBreakingChanges()
			: diffOptions.binaryOnly()
				? report.getBinaryBreakingChanges()
				: report.getBreakingChanges();

		Path ignorePath = diffOptions.ignore();
		if (ignorePath != null && Files.isRegularFile(ignorePath)) {
			IgnoredCsvFile ignoredFile = new IgnoredCsvFile(ignorePath);
			bcs = bcs.stream()
				.filter(bc -> !ignoredFile.isIgnored(bc))
				.toList();
		}

		return new RoseauReport(report.v1(), report.v2(), bcs);
	}

	private void checkOptions(RoseauOptions options) {
		Path v1Path = options.v1().location();

		if (config != null && !Files.isRegularFile(config)) {
			console.printlnErr("Warning: ignoring missing configuration file %s".formatted(config));
		}

		if (sourceOnly != null && binaryOnly != null) {
			throw new RoseauException("Specify either --source-only or --binary-only");
		}

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

		Path pomPath = options.common().classpath().pom();
		if (pomPath != null && !Files.isRegularFile(pomPath)) {
			throw new RoseauException("Cannot find pom: %s".formatted(pomPath));
		}

		Path ignoredPath = options.diff().ignore();
		if (ignoredPath != null && !Files.isRegularFile(ignoredPath)) {
			throw new RoseauException("Cannot find ignored CSV: %s".formatted(ignoredPath));
		}
	}

	private RoseauOptions makeCliOptions() {
		// No CLI option (yet?) for API exclusions
		RoseauOptions.Exclude noExclusions = new RoseauOptions.Exclude(List.of(), List.of());
		RoseauOptions.Common commonCli = new RoseauOptions.Common(
			new RoseauOptions.Classpath(pom, buildClasspathFromString(classpath)), noExclusions);
		RoseauOptions.Library v1Cli = new RoseauOptions.Library(
			v1, new RoseauOptions.Classpath(v1Pom, buildClasspathFromString(v1Classpath)), noExclusions, apiJson);
		RoseauOptions.Library v2Cli = new RoseauOptions.Library(
			v2, new RoseauOptions.Classpath(v2Pom, buildClasspathFromString(v2Classpath)), noExclusions, null);
		RoseauOptions.Diff diffCli = new RoseauOptions.Diff(ignoredCsv, sourceOnly, binaryOnly);
		List<RoseauOptions.Report> reportsCli = (reportPath != null && format != null)
			? List.of(new RoseauOptions.Report(reportPath, format))
			: List.of();
		return new RoseauOptions(commonCli, v1Cli, v2Cli, diffCli, reportsCli);
	}

	private void checkClasspath(Library library) {
		Stopwatch sw = Stopwatch.createStarted();
		if (Files.isRegularFile(library.getPom())) {
			console.printVerbose("Building classpath... ");
		}
		List<Path> classpath = library.getClasspath();
		console.printlnVerbose("%d classpath entries for %s (%d ms)%s".formatted(classpath.size(), library.getLocation(),
			sw.elapsed().toMillis(), classpath.isEmpty() ? ", results may be inaccurate" : ""));
	}

	private void doApi(Library library, RoseauOptions.Library libraryOptions) {
		checkClasspath(library);
		Stopwatch sw = Stopwatch.createStarted();
		console.printVerbose("Building API... ");
		API api = Roseau.buildAPI(library);
		console.printlnVerbose(" %d types (%d ms)".formatted(api.getLibraryTypes().getAllTypes().size(),
			sw.elapsed().toMillis()));
		if (libraryOptions.apiReport() != null) {
			writeApiReport(api, libraryOptions.apiReport());
		}
	}

	private boolean doDiff(Library v1, Library v2, RoseauOptions options) {
		checkClasspath(v1);
		checkClasspath(v2);
		RoseauReport report = filterReport(diff(v1, v2), options.diff());
		console.println(new CliFormatter(plain ? CliFormatter.Mode.PLAIN : CliFormatter.Mode.ANSI).format(filteredReport));

		if (options.v1().apiReport() != null) {
			writeApiReport(report.v1(), options.v1().apiReport());
		}
		if (options.v2().apiReport() != null) {
			writeApiReport(report.v2(), options.v2().apiReport());
		}
		options.reports().forEach(reportOption ->
			writeReport(report, reportOption.format(), reportOption.file())
		);

		return !report.getBreakingChanges().isEmpty();
	}

	@Override
	public Integer call() {
		try {
			Console.Verbosity verbosity = verbosityLevel == null
				? Console.Verbosity.NORMAL
				: switch (verbosityLevel.length) {
					case 0 -> Console.Verbosity.NORMAL;
					case 1 -> Console.Verbosity.VERBOSE;
					default -> Console.Verbosity.DEBUG;
				};
			console = new Console(spec.commandLine().getOut(), spec.commandLine().getErr(), verbosity);

			if (verbosity == Console.Verbosity.DEBUG) {
				Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
			} else if (verbosity == Console.Verbosity.VERBOSE) {
				Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);
			}

			RoseauOptions cliOptions = makeCliOptions();
			RoseauOptions fileOptions = config != null && Files.isRegularFile(config)
				? RoseauOptions.load(config)
				: RoseauOptions.newDefault();
			RoseauOptions options = fileOptions.mergeWith(cliOptions);
			checkOptions(options);
			console.printlnDebug("Options are " + options);

			if (mode.api) {
				Library libraryV1 = options.v1().mergeWith(options.common()).toLibrary();
				console.printlnDebug("v1 = " + libraryV1);
				doApi(libraryV1, options.v1());
			}

			if (mode.diff) {
				Library libraryV1 = options.v1().mergeWith(options.common()).toLibrary();
				Library libraryV2 = options.v2().mergeWith(options.common()).toLibrary();
				console.printlnDebug("v1 = " + libraryV1);
				console.printlnDebug("v2 = " + libraryV2);
				boolean breaking = doDiff(libraryV1, libraryV2, options);

				if (breaking && failMode) {
					return ExitCode.BREAKING.code();
				}
			}

			return ExitCode.SUCCESS.code();
		} catch (RuntimeException e) {
			if (verbosityLevel.length >= Console.Verbosity.VERBOSE.level) {
				console.printStackTrace(e);
			} else {
				String message = Optional.ofNullable(e.getMessage())
					.or(() -> Optional.ofNullable(e.getCause()).map(Throwable::getMessage))
					.orElseGet(() -> e.getClass().getCanonicalName());
				console.printlnErr(message);
				console.printlnErr("Use -v/-vv for detailed error logs.");
			}
			return ExitCode.ERROR.code();
		}
	}

	static void main(String[] args) {
		int exitCode = new CommandLine(new RoseauCLI()).execute(args);
		System.exit(exitCode);
	}

	static final class VersionProvider implements CommandLine.IVersionProvider {
		@Override
		public String[] getVersion() {
			String impl = Optional.ofNullable(Roseau.class.getPackage().getImplementationVersion()).orElse("0.4.0-SNAPSHOT");
			return new String[]{"Roseau " + impl};
		}
	}
}
