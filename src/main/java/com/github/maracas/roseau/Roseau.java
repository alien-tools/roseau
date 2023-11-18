package com.github.maracas.roseau;

import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.google.common.base.Stopwatch;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The `roseau` class is the main entry point of the project.
 */
public class Roseau {
	public static void main(String[] args) throws IOException {
		try (FileWriter writer = new FileWriter("durations_report.csv")) {
			writer.write("Task,Duration\n");

			Stopwatch sw = Stopwatch.createStarted();

			// Spoon parsing
			Launcher launcher1 = launcherFor(Path.of(args[0]));
			Launcher launcher2 = launcherFor(Path.of(args[1]));
			CtModel m1 = launcher1.buildModel();
			CtModel m2 = launcher2.buildModel();

			writer.write("Spoon model building," + sw.elapsed().toMillis() + "\n");
			System.out.println("Spoon model building: " + sw.elapsed().toSeconds());
			sw.reset(); sw.start();

			// API extraction
			SpoonAPIExtractor extractor1 = new SpoonAPIExtractor(m1);
			SpoonAPIExtractor extractor2 = new SpoonAPIExtractor(m2);
			API apiV1 = extractor1.extractAPI();
			API apiV2 = extractor2.extractAPI();

			writer.write("API extraction," + sw.elapsed().toMillis() + "\n");
			System.out.println("API extraction: " + sw.elapsed().toSeconds());
			sw.reset(); sw.start();

			// API serialization
			apiV1.writeJson(Path.of("api-v1.json"));
			apiV1.writeJson(Path.of("api-v2.json"));

			System.out.println("API serialization: " + sw.elapsed().toSeconds());
			sw.reset(); sw.start();

			// API diff
			APIDiff diff = new APIDiff(apiV1, apiV2);
			List<BreakingChange> bcs = diff.diff();

			writer.write("DELTA model," + sw.elapsed().toMillis() + "\n");
			System.out.println("API diff: " + sw.elapsed().toSeconds());

			diff.breakingChangesReport();
		}
	}

	public static Launcher launcherFor(Path location) {
		Launcher launcher;

		if (Files.exists(location.resolve("pom.xml"))) {
			launcher = new MavenLauncher(location.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
		} else {
			launcher = new Launcher();
			launcher.getEnvironment().setComplianceLevel(17);

			launcher.addInputResource(location.toString());
		}

		// Ignore missing types/classpath related errors
		launcher.getEnvironment().setNoClasspath(true);
		// Proceed even if we find the same type twice; affects the precision of the result
		launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
		// Ignore files with syntax/JLS violations and proceed
		launcher.getEnvironment().setIgnoreSyntaxErrors(true);
		// Ignore comments
		launcher.getEnvironment().setCommentEnabled(false);

		return launcher;
	}
}
