package com.github.maracas.roseau.usage;

import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.api.model.Symbol;

public record Use(
	Symbol used,
	UseType type,
	SourceLocation location
) {
	@Override
	public String toString() {
		return "[%s, %s, %s]".formatted(used.getQualifiedName(), type, location);
	}
}
