package com.github.maracas.roseau.api;

import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.SpoonException;
import spoon.reflect.CtModel;
import spoon.support.compiler.SpoonProgress;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class SpoonUtils {
	private static final int JAVA_VERSION = 17;

	private SpoonUtils() {

	}

	public static CtModel buildModel(Path location, Duration timeout) {
		long timeoutSeconds = timeout != null ? timeout.getSeconds() : Long.MAX_VALUE;
		CompletableFuture<CtModel> future = CompletableFuture.supplyAsync(() -> launcherFor(location).buildModel());

		try {
			return future.get(timeoutSeconds, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new SpoonException("Couldn't build Spoon model for %s in < %ds".formatted(location, timeoutSeconds), e);
		} catch (ExecutionException e) {
			throw new SpoonException("Couldn't build Spoon model from " + location, e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SpoonException("Interrupted while building Spoon model from " + location, e);
		}
	}

	public static Launcher launcherFor(Path location) {
		if (!location.toFile().exists())
			throw new IllegalArgumentException(location + " does not exist");

		// Default launcher
		Launcher launcher = new Launcher();
		launcher.addInputResource(location.toString());

		// If we manage to successfully parse it as a Maven project, use that instead
		if (Files.exists(location.resolve("pom.xml"))) {
			MavenLauncher mavenLauncher = new MavenLauncher(location.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE);

			// Fallback if we don't find those
			if (mavenLauncher.getPomFile().getSourceDirectories().isEmpty())
				launcher.addInputResource(location.toString());

			launcher = mavenLauncher;
		}

		// Ignore missing types/classpath related errors
		launcher.getEnvironment().setNoClasspath(true);
		// Proceed even if we find the same type twice; affects the precision of the result
		launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
		launcher.getEnvironment().setComplianceLevel(JAVA_VERSION);
		// Ignore files with syntax/JLS violations and proceed
		launcher.getEnvironment().setIgnoreSyntaxErrors(true);
		// Ignore comments
		launcher.getEnvironment().setCommentEnabled(false);
		// Set Java version
		// Note: even when using the MavenLauncher, it's sometimes not properly inferred, better be safe

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
