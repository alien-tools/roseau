package com.github.maracas.roseau.api.model;

import java.nio.file.Path;

/**
 * A physical source location that points to a specific line in a file
 */
public record SourceLocation(
	Path file,
	int line
) {
	/**
	 * An unknown location for symbols that exist but are not in source code (e.g. default constructors)
	 */
	public static final SourceLocation NO_LOCATION = new SourceLocation(null, -1);
}
