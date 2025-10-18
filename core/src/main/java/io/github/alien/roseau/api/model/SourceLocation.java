package io.github.alien.roseau.api.model;

import java.nio.file.Path;

/**
 * A physical source location that points to a specific line in a file.
 *
 * @param file the pointed file
 * @param line the pointed line in the file
 * @see SourceLocation#NO_LOCATION
 */
public record SourceLocation(
	Path file,
	int line
) {
	/**
	 * An unknown location for symbols that exist but cannot be located in source code (e.g. default constructors)
	 */
	public static final SourceLocation NO_LOCATION = new SourceLocation(null, -1);

	@Override
	public String toString() {
		return (file != null ? file : "<unknown>") + ":" + line;
	}
}
