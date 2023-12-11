package com.github.maracas.roseau;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.google.common.base.Stopwatch;
import picocli.CommandLine;
import spoon.reflect.CtModel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
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

	private void inferAPI(Path sources, Path jsonOutput) throws IOException {
		Stopwatch sw = Stopwatch.createStarted();
		CtModel m = SpoonAPIExtractor.buildModel(sources, SPOON_TIMEOUT)
			.orElseThrow(() -> new RuntimeException("Couldn't build in < 60s"));

		System.out.println("Spoon model building: " + sw.elapsed().toMillis());
		sw.reset();
		sw.start();

		APIExtractor extractor = new SpoonAPIExtractor(m);
		API api = extractor.extractAPI();

		System.out.println("API extraction: " + sw.elapsed().toMillis());
		sw.reset();
		sw.start();

		api.resolve();
		System.out.println("Type resolution: " + sw.elapsed().toMillis());
		sw.reset();
		sw.start();

		// API serialization
		api.writeJson(jsonOutput);
		System.out.println("JSON serialization: " + sw.elapsed().toMillis());
	}

	private void diff(Path v1, Path v2, Path report) throws IOException {
		Stopwatch sw = Stopwatch.createStarted();

		CtModel m1 = SpoonAPIExtractor.buildModel(v1, SPOON_TIMEOUT)
			.orElseThrow(() -> new RuntimeException("Couldn't build in < 60" + SPOON_TIMEOUT));
		CtModel m2 = SpoonAPIExtractor.buildModel(v2, SPOON_TIMEOUT)
			.orElseThrow(() -> new RuntimeException("Couldn't build in < " + SPOON_TIMEOUT));

		System.out.println("Spoon model building: " + sw.elapsed().toMillis());
		sw.reset();
		sw.start();

		// API extraction
		APIExtractor extractor1 = new SpoonAPIExtractor(m1);
		APIExtractor extractor2 = new SpoonAPIExtractor(m2);
		API apiV1 = extractor1.extractAPI();
		API apiV2 = extractor2.extractAPI();

		System.out.println("API extraction: " + sw.elapsed().toMillis());
		sw.reset();
		sw.start();

		// Type resolution
		apiV1.resolve();
		apiV2.resolve();
		System.out.println("Type resolution: " + sw.elapsed().toMillis());
		sw.reset();
		sw.start();

		// API diff
		APIDiff diff = new APIDiff(apiV1, apiV2);
		List<BreakingChange> bcs = diff.diff();

		System.out.println("API diff: " + sw.elapsed().toMillis());

		diff.breakingChangesReport();
		System.out.println(bcs.stream().map(Object::toString).collect(Collectors.joining("\n")));
	}

	@Override
	public Integer call() throws Exception {
		if (apiMode)
			inferAPI(libraryV1, jsonOutput != null ? jsonOutput : Path.of("api.json"));

		if (diffMode)
			diff(libraryV1, libraryV2, report);

		return 0;
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Roseau()).execute(args);
		System.exit(exitCode);
	}
}
