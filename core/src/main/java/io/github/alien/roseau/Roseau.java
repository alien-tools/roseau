package io.github.alien.roseau;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.APIDiff;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatter;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.extractors.asm.AsmAPIExtractor;
import io.github.alien.roseau.extractors.spoon.SpoonAPIExtractor;
import io.github.alien.roseau.extractors.spoon.SpoonUtils;
import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import spoon.reflect.CtModel;

import java.io.IOException;
import java.nio.file.Files;
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

	private static final Logger LOGGER = LogManager.getLogger(Roseau.class);
	private static final Duration SPOON_TIMEOUT = Duration.ofSeconds(60L);
	private static final String RED_TEXT = "\u001B[31m";
	private static final String BOLD = "\u001B[1m";
	private static final String UNDERLINE = "\u001B[4m";
	private static final String RESET = "\u001B[0m";

	private API buildSourcesAPI(Path sources) {
		Stopwatch sw = Stopwatch.createUnstarted();

		// Parsing
		sw.reset().start();
		CtModel model = SpoonUtils.buildModel(sources, SPOON_TIMEOUT);
		LOGGER.info("Parsing {} took {}ms", sources, sw.elapsed().toMillis());

		// API extraction
		SpoonAPIExtractor extractor = new SpoonAPIExtractor();
		sw.reset().start();
		API api = extractor.extractAPI(model);
		LOGGER.info("Extracting API from sources {} took {}ms ({} types)",
			sources, sw.elapsed().toMillis(), api.getExportedTypes().count());

		return api;
	}

	private API buildJarAPI(Path jar) {
		Stopwatch sw = Stopwatch.createUnstarted();

		// API extraction
		AsmAPIExtractor extractor = new AsmAPIExtractor();
		sw.reset().start();
		API api = extractor.extractAPI(jar);
		LOGGER.info("Extracting API from JAR {} took {}ms ({} types)",
			jar, sw.elapsed().toMillis(), api.getExportedTypes().count());

		return api;
	}

	private API buildAPI(Path path) {
		if (path.toFile().isDirectory())
			return buildSourcesAPI(path);
		else
			return buildJarAPI(path);
	}

	private List<BreakingChange> diff(Path v1path, Path v2path) {
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(v1path));
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> buildAPI(v2path));

		CompletableFuture.allOf(futureV1, futureV2).join();

		try {
			API apiV1 = futureV1.get();
			API apiV2 = futureV2.get();

			// API diff
			APIDiff diff = new APIDiff(apiV1, apiV2);
			Stopwatch sw = Stopwatch.createStarted();
			List<BreakingChange> bcs = diff.diff();
			LOGGER.info("API diff took {}ms ({} breaking changes)", sw.elapsed().toMillis(), bcs.size());

			if (reportPath != null) {
				writeReport(bcs);
			}

			return bcs;
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			LOGGER.error("Couldn't compute diff", e);
		}

		return Collections.emptyList();
	}

	private void writeReport(List<BreakingChange> bcs) {
		BreakingChangesFormatter fmt = BreakingChangesFormatterFactory.newBreakingChangesFormatter(format);

		try {
			Files.writeString(reportPath, fmt.format(bcs));
		} catch (IOException e) {
			LOGGER.error("Couldn't write report to {}", reportPath, e);
		}
	}

	private String format(BreakingChange bc) {
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

	@Override
	public Integer call() {
		if (verbose) {
			Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);
		}

		if (apiMode) {
			try {
				API api = buildSourcesAPI(v1);
				api.writeJson(apiPath);
			} catch (IOException e) {
				LOGGER.error("Couldn't write API to {}", apiPath, e);
			}
		}

		if (diffMode) {
			List<BreakingChange> bcs = diff(v1, v2);

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
