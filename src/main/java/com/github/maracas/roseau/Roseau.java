package com.github.maracas.roseau;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.google.common.base.Stopwatch;
import spoon.reflect.CtModel;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * The `roseau` class is the main entry point of the project.
 */
public class Roseau {
	public static final int SPOON_TIMEOUT = 60;

	public static void main(String[] args) {
		try (FileWriter writer = new FileWriter("durations_report.csv")) {
			writer.write("Task,Duration\n");

			Stopwatch sw = Stopwatch.createStarted();

			// Spoon parsing
			CtModel m1 = SpoonAPIExtractor.buildModel(Path.of(args[0]), SPOON_TIMEOUT)
				.orElseThrow(() -> new RuntimeException("Couldn't build in < 60s"));
			CtModel m2 = SpoonAPIExtractor.buildModel(Path.of(args[1]), SPOON_TIMEOUT)
				.orElseThrow(() -> new RuntimeException("Couldn't build in < 60s"));

			writer.write("Spoon model building," + sw.elapsed().toMillis() + "\n");
			System.out.println("Spoon model building: " + sw.elapsed().toSeconds());
			sw.reset();
			sw.start();

			// API extraction
			APIExtractor extractor1 = new SpoonAPIExtractor(m1);
			APIExtractor extractor2 = new SpoonAPIExtractor(m2);
			API apiV1 = extractor1.extractAPI();
			API apiV2 = extractor2.extractAPI();

			writer.write("API extraction," + sw.elapsed().toMillis() + "\n");
			System.out.println("API extraction: " + sw.elapsed().toSeconds());
			sw.reset();
			sw.start();

			// Type resolution
			apiV1.resolve(m1.getRootPackage().getFactory().Type());
			apiV2.resolve(m2.getRootPackage().getFactory().Type());
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
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
