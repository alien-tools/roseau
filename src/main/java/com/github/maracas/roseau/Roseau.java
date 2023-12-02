package com.github.maracas.roseau;

import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.google.common.base.Stopwatch;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.SpoonException;
import spoon.reflect.CtModel;
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
			CtModel m1 = buildModel(Path.of(args[0]), 3);
			CtModel m2 = buildModel(Path.of(args[1]), 3);

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

			// API serialization
			apiV1.writeJson(Path.of("api-v1.json"));
			apiV1.writeJson(Path.of("api-v2.json"));

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

	public static CtModel buildModel(Path location) {
		return buildModel(location, Integer.MAX_VALUE);
	}

	public static CtModel buildModel(Path location, int timeoutSeconds) {
		CompletableFuture<CtModel> future = CompletableFuture.supplyAsync(() -> {
			Launcher launcher = launcherFor(location);
			return launcher.buildModel();
		});

		try {
			return future.get(timeoutSeconds, TimeUnit.SECONDS);
		} catch (TimeoutException | InterruptedException | ExecutionException e) {
			return null;
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

		// Interruptible launcher: this is dirty.
		// Spoon's compiler does two lengthy things: compile units with JDTs,
		// turn these units into Spoon's model. In both cases it iterates
		// over many CUs and reports progress.
		// A simple dirty way to make the process interruptible is to look for
		// interruptions when Spoon reports progress and throw an unchecked
		// exception. The method is called very often, so we're likely to
		// react quickly to external interruptions.
		launcher.getEnvironment().setSpoonProgress(new SpoonProgress() {
			@Override
			public void step(Process process, String task, int taskId, int nbTask) {
				if (Thread.interrupted()) {
					throw new SpoonException("Process interrupted");
				}
			}
		});

		return launcher;
	}
}
