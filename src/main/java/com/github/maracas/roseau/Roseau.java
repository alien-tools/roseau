package com.github.maracas.roseau;

import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.google.common.base.Stopwatch;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.SpoonException;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.SpoonProgress;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The `roseau` class is the main entry point of the project.
 */
public class Roseau {
	public static void main(String[] args) throws IOException {
		try (FileWriter writer = new FileWriter("durations_report.csv")) {
			writer.write("Task,Duration\n");

			Stopwatch sw = Stopwatch.createStarted();

			// Spoon parsing
			CtModel m1 = SpoonAPIExtractor.buildModel(Path.of(args[0]), 60);
			CtModel m2 = SpoonAPIExtractor.buildModel(Path.of(args[1]), 60);

			writer.write("Spoon model building," + sw.elapsed().toMillis() + "\n");
			System.out.println("Spoon model building: " + sw.elapsed().toSeconds());
			sw.reset();
			sw.start();

			// API extraction
			SpoonAPIExtractor extractor1 = new SpoonAPIExtractor(m1);
			SpoonAPIExtractor extractor2 = new SpoonAPIExtractor(m2);
			API apiV1 = extractor1.extractAPI();
			API apiV2 = extractor2.extractAPI();

			writer.write("API extraction," + sw.elapsed().toMillis() + "\n");
			System.out.println("API extraction: " + sw.elapsed().toSeconds());
			sw.reset();
			sw.start();

			// Type resolution
			apiV1.resolve();
			apiV2.resolve();
			System.out.println("Type resolution: " + sw.elapsed().toSeconds());
			sw.reset();
			sw.start();

			// API serialization
			apiV1.writeJson(Path.of("api-v1.json"));
			apiV2.writeJson(Path.of("api-v2.json"));

			System.out.println("API serialization: " + sw.elapsed().toSeconds());
			sw.reset();
			sw.start();

			// API diff
			APIDiff diff = new APIDiff(apiV1, apiV2);
			List<BreakingChange> bcs = diff.diff();

			writer.write("DELTA model," + sw.elapsed().toMillis() + "\n");
			System.out.println("API diff: " + sw.elapsed().toSeconds());

			diff.breakingChangesReport();
			System.out.println(bcs);
		}
	}
}
