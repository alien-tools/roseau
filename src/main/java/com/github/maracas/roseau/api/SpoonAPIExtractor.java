package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.TypeDecl;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.SpoonException;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.SpoonProgress;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * This class represents roseau's API extraction tool.
 * <br/>
 * Types are resolved within the universe of API types (exported or not).
 * We don't know anything about the outside world.
 */
public class SpoonAPIExtractor implements APIExtractor {
	private final CtModel model;

	/**
	 * Constructs an APIExtractor instance with the provided CtModel to extract its information.
	 */
	public SpoonAPIExtractor(CtModel model) {
		this.model = Objects.requireNonNull(model);
	}

	public static Optional<CtModel> buildModel(Path location, int timeoutSeconds) {
		CompletableFuture<Optional<CtModel>> future = CompletableFuture.supplyAsync(() -> {
			Launcher launcher = launcherFor(location);
			return Optional.of(launcher.buildModel());
		});

		try {
			return future.get(timeoutSeconds, TimeUnit.SECONDS);
		} catch (TimeoutException | ExecutionException e) {
			return Optional.empty();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	public static Launcher launcherFor(Path location) {
		Launcher launcher;

		if (Files.exists(location.resolve("pom.xml"))) {
			launcher = new MavenLauncher(location.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE);
		} else {
			launcher = new Launcher();
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
		// Set Java version
		// Note: even when using the MavenLauncher, it's sometimes not properly inferred, better be safe
		launcher.getEnvironment().setComplianceLevel(17);

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

	/**
	 * Extracts the library's (model's) structured API.
	 *
	 * @return Library's (model's) API.
	 */
	public API extractAPI() {
		SpoonAPIFactory factory = new SpoonAPIFactory(model.getRootPackage().getFactory().Type());

		List<TypeDecl> allTypes =
			model.getAllPackages().stream()
				.flatMap(p -> getAllTypes(p).stream().map(factory::convertCtType))
				.toList();

		return new API(allTypes);
	}

	// Returns all types within a package
	private List<CtType<?>> getAllTypes(CtPackage pkg) {
		return pkg.getTypes().stream()
			.flatMap(type -> Stream.concat(
				Stream.of(type),
				getNestedTypes(type).stream()
			))
			.toList();
	}

	// Returns (recursively) nested types within a type
	private List<CtType<?>> getNestedTypes(CtType<?> type) {
		return type.getNestedTypes().stream()
			.flatMap(nestedType -> Stream.concat(
				Stream.of(nestedType),
				getNestedTypes(nestedType).stream()
			))
			.toList();
	}
}
