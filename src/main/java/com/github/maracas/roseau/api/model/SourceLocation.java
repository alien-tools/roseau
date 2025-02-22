package com.github.maracas.roseau.api.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A physical source location that points to a specific line in a file
 */
public record SourceLocation(
	Path file,
	int line
) {
	public SourceLocation {
		Objects.requireNonNull(file);
	}

	/**
	 * An unknown location for symbols that exist but are not in source code (e.g. default constructors)
	 */
	public static final SourceLocation NO_LOCATION = new SourceLocation(Path.of("<unknown>"), -1);

	@Override
	public String toString() {
		return "%s:%s".formatted(file.toAbsolutePath().toString(), line);
	}
}
