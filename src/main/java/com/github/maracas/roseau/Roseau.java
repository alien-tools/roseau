package com.github.maracas.roseau;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import spoon.reflect.CtModel;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
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
		description = "Where to write the breaking changes report; defaults to report.json",
		defaultValue = "report.json")
	private Path reportPath;
	@CommandLine.Option(names = "--verbose",
		description = "Print debug information",
		defaultValue = "false")
	private boolean verbose;

	private static final Logger logger = LogManager.getLogger(Roseau.class);

	private static final int SPOON_TIMEOUT = 60;

	private API buildAPI(Path sources) {
		Stopwatch sw = Stopwatch.createStarted();

		CtModel m = SpoonAPIExtractor.buildModel(sources, SPOON_TIMEOUT)
			.orElseThrow(() -> new RuntimeException("Couldn't build in < " + SPOON_TIMEOUT));

		logger.debug("Parsing: " + sw.elapsed().toMillis());
		sw.reset();
		sw.start();

		// API extraction
		APIExtractor extractor = new SpoonAPIExtractor(m);
		API api = extractor.extractAPI();
		logger.debug("API extraction: " + sw.elapsed().toMillis());

		return api;
	}

	private void diff(Path v1, Path v2, Path report) throws Exception {
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(v1));
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> buildAPI(v2));

		CompletableFuture.allOf(futureV1, futureV2).join();

		API apiV1 = futureV1.get();
		API apiV2 = futureV2.get();

		// API diff
		Stopwatch sw = Stopwatch.createStarted();
		APIDiff diff = new APIDiff(apiV1, apiV2);
		List<BreakingChange> bcs = diff.diff();
		logger.debug("API diff: " + sw.elapsed().toMillis());

		diff.breakingChangesReport(report);
		System.out.println(bcs.stream().map(BreakingChange::format).collect(Collectors.joining("\n")));
	}

	@Override
	public Integer call() throws Exception {
		if (verbose) {
			Configurator.setLevel(logger, Level.DEBUG);
		}

		if (apiMode) {
			API api = buildAPI(libraryV1);
			api.writeJson(apiPath);
		}

		if (diffMode)
			diff(libraryV1, libraryV2, reportPath);

		return 0;
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Roseau()).execute(args);
		System.exit(exitCode);
	}
}
