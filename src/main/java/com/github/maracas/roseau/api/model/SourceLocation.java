package com.github.maracas.roseau.api.model;

import java.nio.file.Path;

public record SourceLocation(
	Path file,
	int line
) {
	public static final SourceLocation NO_LOCATION = new SourceLocation(Path.of("<unknown>"), -1);
}
