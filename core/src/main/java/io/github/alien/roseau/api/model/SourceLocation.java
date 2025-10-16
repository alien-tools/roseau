package io.github.alien.roseau.api.model;

import java.nio.file.Path;
import java.util.Optional;

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
	public SourceLocation(Path file, int line) {
		this.file = Optional.ofNullable(file).map(Path::toAbsolutePath).orElse(null);
		this.line = line;
	}

	/**
	 * An unknown location for symbols that exist but cannot be located in source code (e.g. default constructors)
	 */
	public static final SourceLocation NO_LOCATION = new SourceLocation(null, -1);

	@Override
	public String toString() {
		return (file != null ? file : "<unknown>") + ":" + line;
	}
}
