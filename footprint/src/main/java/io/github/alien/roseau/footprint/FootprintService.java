package io.github.alien.roseau.footprint;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.API;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Generates a single {@code Footprint.java} source file from a source tree.
 */
public final class FootprintService {
	/**
	 * Default package used for generated footprint sources.
	 */
	public static final String DEFAULT_PACKAGE = "io.github.alien.roseau.footprint.generated";

	/**
	 * Default class name used for generated footprint sources.
	 */
	public static final String DEFAULT_CLASS_NAME = "Footprint";

	public String generate(Path sourceTree, String packageName, String className) {
		return generate(sourceTree, Optional.empty(), packageName, className);
	}

	public String generate(Path sourceTree, Path pomFile, String packageName, String className) {
		return generate(sourceTree, Optional.ofNullable(pomFile), packageName, className);
	}

	private String generate(Path sourceTree, Optional<Path> pomFile, String packageName, String className) {
		Objects.requireNonNull(sourceTree);
		Objects.requireNonNull(pomFile);
		Objects.requireNonNull(packageName);
		Objects.requireNonNull(className);

		Library.Builder builder = Library.builder().location(sourceTree.toAbsolutePath().normalize());
		pomFile.map(path -> path.toAbsolutePath().normalize()).ifPresent(builder::pom);
		Library library = builder.build();
		API api = Roseau.buildAPI(library);
		return new FootprintGenerator(packageName, className).generate(api);
	}

	public Path generateToFile(Path sourceTree, Path outputFile, String packageName, String className) throws IOException {
		return generateToFile(sourceTree, outputFile, null, packageName, className);
	}

	public Path generateToFile(Path sourceTree, Path outputFile, Path pomFile, String packageName, String className)
		throws IOException {
		Objects.requireNonNull(outputFile);
		String source = generate(sourceTree, pomFile, packageName, className);

		Path target = outputFile.toAbsolutePath().normalize();
		if (Files.isDirectory(target)) {
			target = target.resolve(className + ".java");
		}

		Path parent = target.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		Files.writeString(target, source, StandardCharsets.UTF_8);
		return target;
	}
}
