package io.github.alien.roseau.cli;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.DiffPolicy;
import io.github.alien.roseau.DiffRequest;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.LibraryResolver;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.diff.formatter.CliFormatter;
import io.github.alien.roseau.options.IgnoredCsvFile;
import io.github.alien.roseau.options.RoseauOptions;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
		"--v1 and --v2 accept JAR files, source code directories, or Maven coordinates (groupId:artifactId:version). " +
		"Example: roseau --diff --v1 /path/to/library-1.0.0.jar --v2 com.example:library:2.0.0",
	footer = {
		"",
		"Output symbols: ✗ removal  ⚠ modification  ★ addition"
	})
public final class RoseauCLI implements Callable<Integer> {
	private Console console;
	@Spec
	private CommandSpec spec;
	@ArgGroup(exclusive = true, multiplicity = "1")
	private Mode mode;

	private static class Mode {
		@Option(names = "--api",
			description = "Serialize the API model of --v1 as JSON; prints to stdout if --api-json is not provided")
		boolean api;
		@Option(names = "--diff",
			description = "Compute breaking changes between versions --v1 and --v2")
		boolean diff;
	}

	@Option(names = "--v1", paramLabel = "<path|coordinates>",
		converter = LibraryVersionConverter.class,
		description = "First version of the library: a JAR file, source directory (e.g., src/main/java), " +
			"or Maven coordinates (e.g., com.example:lib:1.0.0)")
	private LibraryVersion v1;
	@Option(names = "--v2", paramLabel = "<path|coordinates>",
		converter = LibraryVersionConverter.class,
		description = "Second version of the library: a JAR file, source directory (e.g., src/main/java), " +
			"or Maven coordinates (e.g., com.example:lib:2.0.0)")
	private LibraryVersion v2;
	@Option(names = "--api-json", paramLabel = "<path>",
		description = "Where to serialize the Json API model of --v1 in --api mode")
	private Path apiJson;
	@Option(names = "--report", paramLabel = "<format=path>",
		description = "Write a breaking changes report in the given format to the given path; repeatable " +
			"(formats: CLI, CSV, HTML, JSON, MD)",
		converter = ReportOptionConverter.class)
	private List<RoseauOptions.Report> reports;
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
			"this CSV file shares the same structure as a CSV report")
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

	private RoseauReport diff(DiffRequest request) {
		Stopwatch sw = Stopwatch.createStarted();

		console.printVerbose("Building APIs...  ");
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> Roseau.buildAPI(request.v1()));
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> Roseau.buildAPI(request.v2()));
		API apiV1 = futureV1.join();
		API apiV2 = futureV2.join();
		console.printlnVerbose("%d types → %d types (%d ms)".formatted(apiV1.getLibraryTypes().getAllTypes().size(),
			apiV2.getLibraryTypes().getAllTypes().size(), sw.elapsed().toMillis()));

		sw.reset().start();
		console.printVerbose("Comparing APIs... ");
		RoseauReport report = Roseau.diff(apiV1, apiV2).filter(request.policy());
		console.printlnVerbose("%d breaking changes (%d ms)".formatted(report.breakingChanges().size(),
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

	private static final class LibraryVersionConverter implements CommandLine.ITypeConverter<LibraryVersion> {
		private static final Pattern MAVEN_COORDINATES =
			Pattern.compile("[A-Za-z0-9._-]+:[A-Za-z0-9._-]+(:[A-Za-z0-9._-]+)+");

		@Override
		public LibraryVersion convert(String value) {
			if (MAVEN_COORDINATES.matcher(value).matches()) {
				try {
					return new LibraryVersion.MavenCoordinates(ArtifactCoordinates.parse(value));
				} catch (IllegalArgumentException e) {
					throw new CommandLine.TypeConversionException(e.getMessage());
				}
			}
			return new LibraryVersion.LocalPath(Path.of(value));
		}
	}

	private static final class ReportOptionConverter implements CommandLine.ITypeConverter<RoseauOptions.Report> {
		@Override
		public RoseauOptions.Report convert(String value) {
			int separator = value.indexOf('=');
			if (separator <= 0 || separator == value.length() - 1) {
				throw new CommandLine.TypeConversionException("Expected FORMAT=PATH");
			}

			String formatValue = value.substring(0, separator).trim();
			String pathValue = value.substring(separator + 1).trim();
			if (formatValue.isEmpty() || pathValue.isEmpty()) {
				throw new CommandLine.TypeConversionException("Expected FORMAT=PATH");
			}

			try {
				BreakingChangesFormatterFactory format = BreakingChangesFormatterFactory.valueOf(
					formatValue.toUpperCase(Locale.ROOT));
				return new RoseauOptions.Report(Path.of(pathValue), format);
			} catch (IllegalArgumentException e) {
				throw new CommandLine.TypeConversionException("Unknown report format: " + formatValue);
			}
		}
	}

	private void writeApiReport(LibraryTypes types, Path apiPath) {
		try {
			if (apiPath.getParent() != null) {
				Files.createDirectories(apiPath.getParent());
			}
			types.writeJson(apiPath);
			console.printlnVerbose("API has been written to %s".formatted(apiPath));
		} catch (IOException e) {
			throw new RoseauException("Error writing API to %s".formatted(apiPath), e);
		}
	}

	private void checkOptions(RoseauOptions options) {
		Path v1Path = options.v1().location();

		if (config != null && !Files.isRegularFile(config)) {
			console.printlnErr("Warning: ignoring missing configuration file %s".formatted(config));
		}

		if (options.diff().sourceOnly() && options.diff().binaryOnly()) {
			throw new RoseauException("Specify either --source-only or --binary-only");
		}

		if (v1Path == null || !Files.exists(v1Path)) {
			throw new RoseauException("Cannot find v1: %s".formatted(v1Path));
		}

		Path v2Path = options.v2().location();
		if (mode.diff && (v2Path == null || !Files.exists(v2Path))) {
			throw new RoseauException("Cannot find v2: %s".formatted(v2Path));
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

	private Path resolveToPath(LibraryVersion version) {
		if (version == null) {
			return null;
		}
		return switch (version) {
			case LibraryVersion.LocalPath(var path) -> path;
			case LibraryVersion.MavenCoordinates(var coords) -> {
				console.printVerbose("Downloading %s:%s:%s... ".formatted(
					coords.groupId(), coords.artifactId(), coords.version()));
				Path path = ArtifactDownloader.downloadArtifact(coords);
				console.printlnVerbose("done");
				yield path;
			}
		};
	}

	private RoseauOptions makeCliOptions() {
		// No CLI option (yet?) for API exclusions
		RoseauOptions.Exclude noExclusions = new RoseauOptions.Exclude(List.of(), List.of());
		RoseauOptions.Common commonCli = new RoseauOptions.Common(
			new RoseauOptions.Classpath(pom, buildClasspathFromString(classpath)), noExclusions);
		RoseauOptions.Library v1Cli = new RoseauOptions.Library(
			resolveToPath(v1), new RoseauOptions.Classpath(v1Pom, buildClasspathFromString(v1Classpath)), noExclusions, apiJson);
		RoseauOptions.Library v2Cli = new RoseauOptions.Library(
			resolveToPath(v2), new RoseauOptions.Classpath(v2Pom, buildClasspathFromString(v2Classpath)), noExclusions, null);
		boolean cliSourceOnly = Boolean.TRUE.equals(sourceOnly);
		boolean cliBinaryOnly = Boolean.TRUE.equals(binaryOnly);
		RoseauOptions.Diff diffCli = new RoseauOptions.Diff(ignoredCsv, cliSourceOnly, cliBinaryOnly);
		List<RoseauOptions.Report> reportsCli = reports == null ? List.of() : List.copyOf(reports);
		return new RoseauOptions(commonCli, v1Cli, v2Cli, diffCli, reportsCli);
	}

	private void buildClasspath(Library library) {
		List<Path> classpath = library.classpath();
		console.printlnVerbose("%d classpath entries for %s".formatted(classpath.size(), library.location()));

		if (classpath.isEmpty()) {
			console.printlnErr("Warning: no classpath provided, results may be inaccurate");
		}
	}

	private void doApi(Library library, RoseauOptions.Library libraryOptions) {
		buildClasspath(library);
		Stopwatch sw = Stopwatch.createStarted();
		console.printVerbose("Extracting API... ");
		LibraryTypes types = Roseau.buildLibraryTypes(library);
		console.printlnVerbose(" %d types (%d ms)".formatted(types.getAllTypes().size(),
			sw.elapsed().toMillis()));
		if (libraryOptions.apiReport() != null) {
			writeApiReport(types, libraryOptions.apiReport());
		} else {
			try {
				console.println(types.toJson());
			} catch (IOException e) {
				throw new RoseauException("Error printing the API", e);
			}
		}
	}

	private boolean doDiff(DiffRequest request, RoseauOptions options) {
		buildClasspath(request.v1());
		buildClasspath(request.v2());
		RoseauReport report = diff(request);
		console.println(new CliFormatter(plain ? CliFormatter.Mode.PLAIN : CliFormatter.Mode.ANSI).format(report));

		if (options.v1().apiReport() != null) {
			writeApiReport(report.v1().getLibraryTypes(), options.v1().apiReport());
		}
		if (options.v2().apiReport() != null) {
			writeApiReport(report.v2().getLibraryTypes(), options.v2().apiReport());
		}
		options.reports().forEach(reportOption ->
			report.writeReport(reportOption.format(), reportOption.file())
		);

		return !report.breakingChanges().isEmpty();
	}

	private Library buildLibrary(RoseauOptions.Library libraryOptions, RoseauOptions.Common common) {
		RoseauOptions.Library merged = libraryOptions.mergeWith(common);
		return new LibraryResolver().resolve(
			merged.location(),
			merged.classpath().jars(),
			merged.classpath().pom()
		);
	}

	private DiffPolicy buildDiffPolicy(RoseauOptions options) {
		RoseauOptions.Library baselineOptions = options.v1().mergeWith(options.common());
		List<Pattern> namePatterns = baselineOptions.excludes().names().stream()
			.<Pattern>mapMulti((name, downstream) -> {
				try {
					downstream.accept(Pattern.compile(name));
				} catch (PatternSyntaxException e) {
					console.printlnErr("Invalid name exclusion: %s (%s)".formatted(name, e.getMessage()));
				}
			})
			.toList();

		DiffPolicy.Builder builder = DiffPolicy.builder()
			.scope(toScope(options.diff()))
			.excludeNames(namePatterns)
			.excludeAnnotations(baselineOptions.excludes().annotations().stream()
				.map(ann -> new DiffPolicy.AnnotationExclusion(ann.name(), ann.args()))
				.toList());

		Path ignoredPath = options.diff().ignore();
		if (ignoredPath != null && Files.isRegularFile(ignoredPath)) {
			builder.ignoreBreakingChanges(new IgnoredCsvFile(ignoredPath).ignoredBreakingChanges());
		}

		return builder.build();
	}

	private static DiffPolicy.Scope toScope(RoseauOptions.Diff diff) {
		if (Boolean.TRUE.equals(diff.sourceOnly())) {
			return DiffPolicy.Scope.SOURCE_ONLY;
		}
		if (Boolean.TRUE.equals(diff.binaryOnly())) {
			return DiffPolicy.Scope.BINARY_ONLY;
		}
		return DiffPolicy.Scope.ALL;
	}

	@Override
	public Integer call() {
		Console.Verbosity verbosity = verbosityLevel == null
			? Console.Verbosity.NORMAL
			: switch (verbosityLevel.length) {
			case 0 -> Console.Verbosity.NORMAL;
			case 1 -> Console.Verbosity.VERBOSE;
			default -> Console.Verbosity.DEBUG;
		};

		try {
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
				Library libraryV1 = buildLibrary(options.v1(), options.common());
				console.printlnDebug("v1 = " + libraryV1);
				doApi(libraryV1, options.v1());
			}

			if (mode.diff) {
				Library libraryV1 = buildLibrary(options.v1(), options.common());
				Library libraryV2 = buildLibrary(options.v2(), options.common());
				DiffPolicy policy = buildDiffPolicy(options);
				console.printlnDebug("v1 = " + libraryV1);
				console.printlnDebug("v2 = " + libraryV2);
				console.printlnDebug("diff policy = " + policy.scope());
				boolean breaking = doDiff(new DiffRequest(libraryV1, libraryV2, policy), options);

				if (breaking && failMode) {
					return ExitCode.BREAKING.code();
				}
			}

			return ExitCode.SUCCESS.code();
		} catch (RuntimeException e) {
			if (verbosity.level >= Console.Verbosity.VERBOSE.level) {
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
		private static final String VERSION_RESOURCE = "/roseau-version.txt";

		@Override
		public String[] getVersion() {
			return new String[]{"Roseau " + resolveVersion()};
		}

		static String resolveVersion() {
			return Optional.ofNullable(RoseauCLI.class.getPackage().getImplementationVersion())
				.or(VersionProvider::readVersionResource)
				.orElse("unknown");
		}

		private static Optional<String> readVersionResource() {
			try (InputStream in = RoseauCLI.class.getResourceAsStream(VERSION_RESOURCE)) {
				if (in == null) {
					return Optional.empty();
				}

				String version = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
				return version.isEmpty() ? Optional.empty() : Optional.of(version);
			} catch (IOException e) {
				return Optional.empty();
			}
		}
	}
}
