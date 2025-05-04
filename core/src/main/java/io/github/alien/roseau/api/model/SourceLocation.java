package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;

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
	public SourceLocation(Path file, int line) {
		Preconditions.checkNotNull(file);
		this.file = file.toAbsolutePath();
		this.line = line;
	}

	/**
	 * An unknown location for symbols that exist but cannot be located in source code (e.g. default constructors)
	 */
	public static final SourceLocation NO_LOCATION = new SourceLocation(Path.of("<unknown>"), -1);

	@Override
	public String toString() {
		return file + ":" + line;
	}
}
