package io.github.alien.roseau.extractors.spoon;

import io.github.alien.roseau.RoseauException;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.support.compiler.SpoonProgress;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A collection of utilities for parsing and building models using Spoon.
 */
public final class SpoonUtils {
	private static final int JAVA_VERSION = 21;

	private SpoonUtils() {

	}

	/**
	 * Builds a Spoon model from the source code located at the given path, with a specified timeout.
	 *
	 * @param location  The path to the source code
	 * @param classpath The classpath used to resolve references
	 * @param timeout   The maximum duration to wait for the model to be built
	 * @return The built Spoon model
	 * @throws RoseauException If there is an error in building the Spoon model
	 */
	public static CtModel buildModel(Path location, Collection<Path> classpath, Duration timeout) {
		return buildModel(launcherFor(location, classpath), location, timeout);
	}

	/**
	 * Builds a Spoon model from the source code located at the given path using the supplied launcher, with a specified
	 * timeout.
	 *
	 * @param launcher The launcher used to build a {@link CtModel}
	 * @param location The path to the source code
	 * @param timeout  The maximum duration to wait for the model to be built
	 * @return The built Spoon model
	 * @throws RoseauException If there is an error in building the Spoon model
	 */
	public static CtModel buildModel(Launcher launcher, Path location, Duration timeout) {
		long timeoutSeconds = timeout != null ? timeout.getSeconds() : Long.MAX_VALUE;
		CompletableFuture<CtModel> future = CompletableFuture.supplyAsync(launcher::buildModel);

		try {
			return future.get(timeoutSeconds, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new RoseauException("Couldn't build Spoon model for %s in < %ds".formatted(location, timeoutSeconds), e);
		} catch (ExecutionException e) {
			throw new RoseauException("Couldn't build Spoon model from " + location, e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RoseauException("Interrupted while building Spoon model from " + location, e);
		}
	}

	/**
	 * Creates a Spoon Launcher for the given location. The resulting launcher is interruptible.
	 *
	 * @param location  The path to the source code
	 * @param classpath The classpath used to resolve references
	 * @return The created Spoon Launcher, either regular or Maven-specific
	 * @throws IllegalArgumentException if the specified location does not exist
	 */
	public static Launcher launcherFor(Path location, Collection<Path> classpath) {
		if (!location.toFile().exists()) {
			throw new IllegalArgumentException(location + " does not exist");
		}

		Launcher launcher = new Launcher();
		launcher.addInputResource(location.toString());
		launcher.getEnvironment().setSourceClasspath(
			classpath.stream()
				.map(Path::toAbsolutePath)
				.map(Path::toString)
				.toArray(String[]::new));

		// If we manage to successfully parse it as a Maven project, use that instead
		if (Files.exists(location.resolve("pom.xml"))) {
			MavenLauncher mavenLauncher = new MavenLauncher(location.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE);

			// Fallback if we don't find those
			if (mavenLauncher.getPomFile().getSourceDirectories().isEmpty()) {
				launcher.addInputResource(location.toString());
			}

			launcher = mavenLauncher;
		}

		// Set log level; messages are redirected to log4j with our own independent level
		launcher.getEnvironment().setLevel("ERROR");
		// Ignore missing types/classpath related errors
		launcher.getEnvironment().setNoClasspath(true);
		// Proceed even if we find the same type twice; affects the precision of the result.
		// Caution: this will make the not-so-safe generics typecasts break if two types
		// of different kinds (e.g. class vs interface) exist in our sources
		// launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
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
					throw new RoseauException("Process interrupted");
				}
			}
		});

		return launcher;
	}
}
