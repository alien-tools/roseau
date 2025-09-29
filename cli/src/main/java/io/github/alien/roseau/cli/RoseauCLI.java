package io.github.alien.roseau.cli;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatter;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.extractors.ExtractorType;
import io.github.alien.roseau.extractors.MavenClasspathBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

/**
 * Main class implementing a CLI for interacting with Roseau. See {@code --help} for usage information.
 */
@Command(name = "roseau")
public final class RoseauCLI implements Callable<Integer> {
	@Spec
	CommandSpec spec;
	@Option(names = "--api",
		description = "Serialize the API model of --v1; see --json")
	private boolean apiMode;
	@Option(names = "--diff",
		description = "Compute breaking changes between versions --v1 and --v2")
	private boolean diffMode;
	@Option(names = "--v1",
		description = "Path to the first version of the library; either a source directory or a JAR",
		required = true)
	private Path v1;
	@Option(names = "--v2",
		description = "Path to the second version of the library; either a source directory or a JAR")
	private Path v2;
	@Option(names = "--extractor",
		description = "API extractor to use: ${COMPLETION-CANDIDATES}")
	private ExtractorType extractorType;
	@Option(names = "--json",
		description = "Where to serialize the JSON API model of --v1; defaults to api.json",
		defaultValue = "api.json")
	private Path apiPath;
	@Option(names = "--report",
		description = "Where to write the breaking changes report")
	private Path reportPath;
	@Option(names = "--verbose",
		description = "Print debug information")
	private boolean verbose;
	@Option(names = "--fail",
		description = "Return a non-zero code if breaking changes are detected")
	private boolean failMode;
	@Option(names = "--format",
		description = "Format of the report; possible values: ${COMPLETION-CANDIDATES}",
		defaultValue = "CSV")
	private BreakingChangesFormatterFactory format;
	@Option(names = "--pom",
		description = "A pom.xml file to build a classpath from")
	private Path pom;
	@Option(names = "--classpath", split = ":", paramLabel = "<jar>",
		description = "A colon-separated list of JARs to include in the classpath")
	private List<Path> userClasspath = List.of();
	@Option(names = "--plain",
		description = "Disable ANSI colors, output plain text")
	private boolean plain;
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

		writeReport(apiV1, report);
		return report;
	}

	private List<Path> buildClasspath() {
		List<Path> classpath = new ArrayList<>(userClasspath);

		if (pom != null && Files.isRegularFile(pom)) {
			Stopwatch sw = Stopwatch.createStarted();
			MavenClasspathBuilder classpathBuilder = new MavenClasspathBuilder();
			classpath.addAll(classpathBuilder.buildClasspath(pom));
			printVerbose("Extracting classpath from %s took %dms".formatted(pom, sw.elapsed().toMillis()));
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
			Files.writeString(reportPath, fmt.format(api, report));
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

	private void checkArguments() {
		if (v1 == null || !Files.exists(v1)) {
			throw new IllegalArgumentException("Cannot find v1: %s".formatted(v1));
		}

		if (diffMode && (v2 == null || !Files.exists(v2))) {
			throw new IllegalArgumentException("Cannot find v2: %s".formatted(v2));
		}

		if (pom != null && !Files.exists(pom)) {
			throw new IllegalArgumentException("Cannot find pom: %s".formatted(pom));
		}
	}

	@Override
	public Integer call() {
		try {
			if (verbose) {
				Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
			}

			checkArguments();
			List<Path> classpath = buildClasspath();
			Library.Builder builder1 = Library.builder()
				.location(v1)
				.classpath(classpath);
			if (extractorType != null) {
				builder1.extractorType(extractorType);
			}
			Library libraryV1 = builder1.build();

			if (apiMode) {
				API api = buildAPI(libraryV1);
				writeApiReport(api);
			}

			if (diffMode) {
				Library.Builder builder2 = Library.builder()
					.location(v2)
					.classpath(classpath);
				if (extractorType != null) {
					builder2.extractorType(extractorType);
				}
				Library libraryV2 = builder2.build();
				List<BreakingChange> bcs = diff(libraryV1, libraryV2).breakingChanges();

				if (bcs.isEmpty()) {
					print("No breaking changes found.");
				} else {
					print(
						bcs.stream()
							.map(this::format)
							.collect(Collectors.joining(System.lineSeparator()))
					);

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
			return 1;
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
