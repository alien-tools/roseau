package com.github.maracas.roseau;

import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.SpoonUtils;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.diff.formatter.BreakingChangesFormatter;
import com.github.maracas.roseau.diff.formatter.BreakingChangesFormatterFactory;
import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import spoon.reflect.CtModel;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandLine.Command(name = "roseau")
final class Roseau implements Callable<Integer>  {
	@CommandLine.Option(names = "--api",
		description = "Build and serialize the API model of --v1")
	private boolean apiMode;
	@CommandLine.Option(names = "--diff",
		description = "Compute the breaking changes between versions --v1 and --v2")
	private boolean diffMode;
	@CommandLine.Option(names = "--v1",
		description = "Path to the sources of the first version of the library", required = true)
	private Path libraryV1;
	@CommandLine.Option(names = "--v2",
		description = "Path to the sources of the second version of the library")
	private Path libraryV2;
	@CommandLine.Option(names = "--json",
		description = "Where to serialize the JSON API model of --v1; defaults to api.json",
		defaultValue = "api.json")
	private Path apiPath;
	@CommandLine.Option(names = "--report",
		description = "Where to write the breaking changes report; defaults to report",
		defaultValue = "report")
	private Path reportPath;
	@CommandLine.Option(names = "--verbose",
		description = "Print debug information",
		defaultValue = "false")
	private boolean verbose;
	@CommandLine.Option(names = "--fail",
			description = "Command returns an error when there are breaking changes detected",
			defaultValue = "false")
	private boolean failMode;
	@CommandLine.Option(names = "--format",
			description="Format of the report; defaults to csv; possible values: ${COMPLETION-CANDIDATES}",
			defaultValue="CSV")
	private BreakingChangesFormatterFactory format;

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Duration SPOON_TIMEOUT = Duration.ofSeconds(60L);
	private static final String RED_TEXT = "\u001B[31m";
	private static final String BOLD = "\u001B[1m";
	private static final String UNDERLINE = "\u001B[4m";
	private static final String RESET = "\u001B[0m";

	private API buildAPI(Path sources) {
		Stopwatch sw = Stopwatch.createStarted();

		// Parsing
		CtModel model = SpoonUtils.buildModel(sources, SPOON_TIMEOUT);
		LOGGER.info("Parsing {} took {}ms", sources, sw.elapsed().toMillis());
		sw.reset();
		sw.start();

		// API extraction
		SpoonAPIExtractor extractor = new SpoonAPIExtractor();
		API api = extractor.extractAPI(model);
		LOGGER.info("Extracting API for {} took {}ms ({} types)", sources, sw.elapsed().toMillis(), api.getExportedTypes().count());

		return api;
	}

	private List<BreakingChange> diff(Path v1, Path v2, Path report) {
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(v1));
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> buildAPI(v2));

		CompletableFuture.allOf(futureV1, futureV2).join();

		try {
			API apiV1 = futureV1.get();
			API apiV2 = futureV2.get();

			// API diff
			Stopwatch sw = Stopwatch.createStarted();
			APIDiff diff = new APIDiff(apiV1, apiV2);
			List<BreakingChange> bcs = diff.diff();
			LOGGER.info("API diff took {}ms ({} breaking changes)", sw.elapsed().toMillis(), bcs.size());

			BreakingChangesFormatter fmt = BreakingChangesFormatterFactory.newBreakingChangesFormatter(format);
			if (!hasGoodExtension(report, fmt.getFileExtension()))
				report = modifyReportExtension(report, fmt.getFileExtension());
			try (FileWriter writer = new FileWriter(report.toFile(), StandardCharsets.UTF_8)) {
				writer.write(fmt.format(bcs));
			}

			return bcs;
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			LOGGER.error("Couldn't compute diff", e);
		} catch (IOException e) {
			LOGGER.error("Couldn't write diff to {}", report, e);
		}

		return Collections.emptyList();
	}

	private static boolean hasGoodExtension(Path report, String extension) {
		return report.getFileName().toString().endsWith("." + extension);
	}

	private static Path modifyReportExtension(Path report, String extension) {
		return report.resolveSibling(report.getFileName() + "." + extension);
	}

	private String format(BreakingChange bc) {
		return String.format("%s %s%n\t%s:%s",
			RED_TEXT + BOLD + bc.kind() + RESET,
			UNDERLINE + bc.impactedSymbol().getQualifiedName() + RESET,
			bc.impactedSymbol().getLocation() == SourceLocation.NO_LOCATION ? "unknown" : libraryV1.toAbsolutePath().relativize(bc.impactedSymbol().getLocation().file()),
			bc.impactedSymbol().getLocation() == SourceLocation.NO_LOCATION ? "unknown" : bc.impactedSymbol().getLocation().line());
	}

	@Override
	public Integer call() {
		if (verbose) {
			Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);
		}

		if (apiMode) {
			try {
				API api = buildAPI(libraryV1);
				api.writeJson(apiPath);
			} catch (IOException e) {
				LOGGER.error("Couldn't write API to {}", apiPath, e);
			}
		}

		if (diffMode) {
			List<BreakingChange> bcs = diff(libraryV1, libraryV2, reportPath);

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
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Roseau()).execute(args);
		System.exit(exitCode);
	}
}
