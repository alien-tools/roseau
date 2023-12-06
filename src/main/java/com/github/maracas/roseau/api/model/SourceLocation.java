package com.github.maracas.roseau.api.model;

import java.nio.file.Path;

public record SourceLocation(
	Path file,
	int line
) {
	public static final SourceLocation NO_LOCATION = new SourceLocation(null, -1);
}
