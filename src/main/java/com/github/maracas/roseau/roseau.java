package com.github.maracas.roseau;

import com.github.maracas.roseau.changes.BreakingChange;
import com.github.maracas.roseau.model.API;
import spoon.Launcher;
import spoon.MavenLauncher;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The `roseau` class is the main entry point of the project.
 */
public class roseau {
	public static void main(String[] args) {
		try {
			try (FileWriter writer = new FileWriter("durations_report.csv")) {
				writer.write("Task,Duration\n");

				String path1 = args[0];
				String path2 = args[1];

				Path v1 = Path.of(path1);
				Launcher launcher1 = launcherFor(v1);
				Path v2 = Path.of(path2);
				Launcher launcher2 = launcherFor(v2);

				long startTime = System.nanoTime();

				APIExtractor extractor1 = new APIExtractor(launcher1.buildModel());
				APIExtractor extractor2 = new APIExtractor(launcher2.buildModel());

				long endTime = System.nanoTime();
				long duration = (endTime - startTime) / 1000000;
				writer.write("Spoon model building" + "," + duration + "\n");

				System.out.println(" Spoon model building : " + duration + "ms");

				startTime = System.nanoTime();

				API apiV1 = extractor1.extractingAPI();
				API apiV2 = extractor2.extractingAPI();

				System.out.println(apiV1);

				endTime = System.nanoTime();
				duration = (endTime - startTime) / 1000000;
				System.out.println(" API extraction : " + duration + "ms");
				writer.write("API extraction" + "," + duration + "\n");

				startTime = System.nanoTime();

				APIDiff diff = new APIDiff(apiV1, apiV2);
				List<BreakingChange> breakingChanges = diff.getBreakingChanges();

				endTime = System.nanoTime();
				duration = (endTime - startTime) / 1000000;
				System.out.println(" DELTA model : " + duration + "ms");

				writer.write("DELTA model" + "," + duration + "\n");

				diff.breakingChangesReport();
				System.out.println(diff.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (spoon.SpoonException e) {
			System.err.println(" Please provide two valid paths ");
		}
	}

	public static Launcher launcherFor(Path location) {
		Launcher launcher;

		if (Files.exists(location.resolve("pom.xml"))) {
			launcher = new MavenLauncher(location.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
		} else {
			launcher = new Launcher();
			launcher.getEnvironment().setComplianceLevel(11);

			launcher.addInputResource(location.toString());
		}

		// Ignore missing types/classpath related errors
		launcher.getEnvironment().setNoClasspath(true);
		// Proceed even if we find the same type twice; affects the precision of the result
		launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
		// Ignore files with syntax/JLS violations and proceed
		launcher.getEnvironment().setIgnoreSyntaxErrors(true);

		return launcher;
	}
}
