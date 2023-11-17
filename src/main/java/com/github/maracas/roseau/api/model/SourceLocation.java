package com.github.maracas.roseau.api.model;

import java.nio.file.Path;

public record SourceLocation(
	Path file,
	int beginLine,
	int beginColumn,
	int endLine,
	int endColumn
) {
	public final static SourceLocation NO_LOCATION = new SourceLocation(Path.of("<unknown>"), -1, -1, -1, -1);
}
