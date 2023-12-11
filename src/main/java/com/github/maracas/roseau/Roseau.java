package com.github.maracas.roseau;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.google.common.base.Stopwatch;
import picocli.CommandLine;
import spoon.reflect.CtModel;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * The `roseau` class is the main entry point of the project.
 */
@CommandLine.Command(name = "roseau")
final class Roseau implements Callable<Integer>  {
	@CommandLine.Option(names = "--api")
	private boolean apiMode;
	@CommandLine.Option(names = "--diff")
	private boolean diffMode;
	@CommandLine.Option(names = "--v1", required = true)
	private Path libraryV1;
	@CommandLine.Option(names = "--v2")
	private Path libraryV2;
	@CommandLine.Option(names = "--json")
	private Path jsonOutput;
	@CommandLine.Option(names = "--report")
	private Path report;

	private static final int SPOON_TIMEOUT = 60;

	private API buildAPI(Path sources) {
		Stopwatch sw = Stopwatch.createStarted();

		CtModel m = SpoonAPIExtractor.buildModel(sources, SPOON_TIMEOUT)
			.orElseThrow(() -> new RuntimeException("Couldn't build in < " + SPOON_TIMEOUT));

		System.out.println("Parsing: " + sw.elapsed().toMillis());
		sw.reset();
		sw.start();

		// API extraction
		APIExtractor extractor = new SpoonAPIExtractor(m);
		API api = extractor.extractAPI();

		System.out.println("API extraction: " + sw.elapsed().toMillis());
		sw.reset();
		sw.start();

		// API resolution
		api.resolve();
		System.out.println("API resolution: " + sw.elapsed().toMillis());
		return api;
	}

	private void diff(Path v1, Path v2, Path report) throws Exception {
		Stopwatch sw = Stopwatch.createStarted();

		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(v1));
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> buildAPI(v2));

		CompletableFuture.allOf(futureV1, futureV2).join();

		API apiV1 = futureV1.get();
		API apiV2 = futureV2.get();

		// API diff
		APIDiff diff = new APIDiff(apiV1, apiV2);
		List<BreakingChange> bcs = diff.diff();

		System.out.println("API diff: " + sw.elapsed().toMillis());

		diff.breakingChangesReport();
		System.out.println(bcs.stream().map(Object::toString).collect(Collectors.joining("\n")));
	}

	@Override
	public Integer call() throws Exception {
		if (apiMode) {
			API api = buildAPI(libraryV1);
			api.writeJson(jsonOutput != null ? jsonOutput : Path.of("api.json"));
		}

		if (diffMode)
			diff(libraryV1, libraryV2, report);

		return 0;
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Roseau()).execute(args);
		System.exit(exitCode);
	}
}
