package io.github.alien.roseau.cli;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.APIDiff;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatter;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.extractors.MavenClasspathBuilder;
import io.github.alien.roseau.extractors.TypesExtractor;
import io.github.alien.roseau.extractors.TypesExtractorFactory;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Main class implementing a CLI for interacting with Roseau. See {@code --help} for usage information.
 */
@CommandLine.Command(name = "roseau")
public final class RoseauCLI implements Callable<Integer> {
	@CommandLine.Option(names = "--api",
		description = "Serialize the API model of --v1; see --json")
	private boolean apiMode;
	@CommandLine.Option(names = "--diff",
		description = "Compute breaking changes between versions --v1 and --v2")
	private boolean diffMode;
	@CommandLine.Option(names = "--v1",
		description = "Path to the first version of the library; either a source directory or a JAR",
		required = true)
	private Path v1;
	@CommandLine.Option(names = "--v2",
		description = "Path to the second version of the library; either a source directory or a JAR")
	private Path v2;
	@CommandLine.Option(names = "--extractor",
		description = "API extractor to use: ${COMPLETION-CANDIDATES}",
		defaultValue = "JDT")
	private TypesExtractorFactory extractorFactory;
	@CommandLine.Option(names = "--json",
		description = "Where to serialize the JSON API model of --v1; defaults to api.json",
		defaultValue = "api.json")
	private Path apiPath;
	@CommandLine.Option(names = "--report",
		description = "Where to write the breaking changes report")
	private Path reportPath;
	@CommandLine.Option(names = "--verbose",
		description = "Print debug information")
	private boolean verbose;
	@CommandLine.Option(names = "--fail",
		description = "Return a non-zero code if breaking changes are detected")
	private boolean failMode;
	@CommandLine.Option(names = "--format",
		description = "Format of the report; possible values: ${COMPLETION-CANDIDATES}",
		defaultValue = "CSV")
	private BreakingChangesFormatterFactory format;
	@CommandLine.Option(names = "--pom",
		description = "A pom.xml file to build a classpath from")
	private Path pom;
	@CommandLine.Option(names = "--classpath",
		description = "A colon-separated list of elements to include in the classpath")
	private String classpathString;
	@CommandLine.Option(names = "--plain",
		description = "Disable ANSI colors, output plain text")
	private boolean plain;

	private static final Logger LOGGER = LogManager.getLogger(RoseauCLI.class);
	private static final String RED_TEXT = "\u001B[31m";
	private static final String BOLD = "\u001B[1m";
	private static final String UNDERLINE = "\u001B[4m";
	private static final String RESET = "\u001B[0m";

	private API buildAPI(Path sources, List<Path> classpath) {
		TypesExtractor extractor = sources.toString().endsWith(".jar")
			? new AsmTypesExtractor()
			: TypesExtractorFactory.newExtractor(extractorFactory);

		if (extractor.canExtract(sources)) {
			Stopwatch sw = Stopwatch.createStarted();
			API api = extractor.extractTypes(sources, classpath).toAPI(classpath);
			LOGGER.debug("Extracting API from sources {} using {} took {}ms ({} types)",
				sources, extractor.getName(), sw.elapsed().toMillis(), api.getExportedTypes().size());
			return api;
		} else {
			throw new RoseauException("Extractor %s does not support sources %s".formatted(extractor.getName(), sources));
		}
	}

	private List<BreakingChange> diff(Path v1path, Path v2path, List<Path> classpath) {
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(v1path, classpath));
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> buildAPI(v2path, classpath));

		CompletableFuture.allOf(futureV1, futureV2).join();

		try {
			API apiV1 = futureV1.get();
			API apiV2 = futureV2.get();

			// API diff
			APIDiff diff = new APIDiff(apiV1, apiV2);
			Stopwatch sw = Stopwatch.createStarted();
			List<BreakingChange> bcs = diff.diff();
			LOGGER.debug("API diff took {}ms ({} breaking changes)", sw.elapsed().toMillis(), bcs.size());

			writeReport(apiV1, bcs);
			return bcs;
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			LOGGER.error("Couldn't compute diff", e);
		}

		return Collections.emptyList();
	}

	private List<Path> buildClasspath() {
		List<Path> classpath = new ArrayList<>();
		if (pom != null && Files.isRegularFile(pom)) {
			Stopwatch sw = Stopwatch.createStarted();
			MavenClasspathBuilder classpathBuilder = new MavenClasspathBuilder();
			classpath.addAll(classpathBuilder.buildClasspath(pom));
			LOGGER.debug("Extracting classpath from {} took {}ms", pom, sw.elapsed().toMillis());
		}

		if (!Strings.isNullOrEmpty(classpathString)) {
			classpath.addAll(Arrays.stream(classpathString.split(":"))
				.map(Path::of)
				.toList());
		}

		if (classpath.isEmpty()) {
			LOGGER.warn("No classpath provided, results may be inaccurate");
		} else {
			LOGGER.debug("Classpath: {}", classpath);
		}

		return classpath;
	}

	private void writeReport(API api, List<BreakingChange> bcs) {
		if (reportPath == null)
			return;

		BreakingChangesFormatter fmt = BreakingChangesFormatterFactory.newBreakingChangesFormatter(format);

		try {
			Files.writeString(reportPath, fmt.format(api, bcs));
			LOGGER.info("Wrote report to {}", reportPath);
		} catch (IOException e) {
			LOGGER.error("Couldn't write report to {}", reportPath, e);
		}
	}

	private String format(BreakingChange bc) {
		if (plain) {
			return String.format("%s %s%n\t%s:%s", bc.kind(), bc.impactedSymbol().getQualifiedName(),
				bc.impactedSymbol().getLocation().file(), bc.impactedSymbol().getLocation().line());
		} else {
			return String.format("%s %s%n\t%s:%s",
				RED_TEXT + BOLD + bc.kind() + RESET,
				UNDERLINE + bc.impactedSymbol().getQualifiedName() + RESET,
				bc.impactedSymbol().getLocation() == SourceLocation.NO_LOCATION
					? "unknown"
					: v1.toAbsolutePath().relativize(bc.impactedSymbol().getLocation().file()),
				bc.impactedSymbol().getLocation() == SourceLocation.NO_LOCATION
					? "unknown"
					: bc.impactedSymbol().getLocation().line());
		}
	}

	private void checkArguments() {
		if (v1 == null || !Files.exists(v1)) {
			throw new IllegalArgumentException("--v1 does not exist");
		}

		if (diffMode && (v2 == null || !Files.exists(v2))) {
			throw new IllegalArgumentException("--v2 does not exist");
		}

		if (pom != null && !Files.exists(pom)) {
			throw new IllegalArgumentException("--pom does not exist");
		}
	}

	@Override
	public Integer call() {
		if (verbose) {
			Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
		}

		try {
			checkArguments();
			List<Path> classpath = buildClasspath();

			if (apiMode) {
				API api = buildAPI(v1, classpath);
				api.getLibraryTypes().writeJson(apiPath);
				LOGGER.info("Wrote API to {}", apiPath);
			}

			if (diffMode) {
				List<BreakingChange> bcs = diff(v1, v2, classpath);

				if (bcs.isEmpty()) {
					System.out.println("No breaking changes found.");
				} else {
					System.out.println(
						bcs.stream()
							.map(this::format)
							.collect(Collectors.joining(System.lineSeparator()))
					);
				}

				if (failMode && !bcs.isEmpty()) {
					return 1;
				}
			}

			return 0;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return 1;
		}
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new RoseauCLI()).execute(args);
		System.exit(exitCode);
	}
}
