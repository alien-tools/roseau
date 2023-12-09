package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;

public record ParameterDecl(
	String name,
	ITypeReference type,
	boolean isVarargs
) {
	@Override
	public String toString() {
		return "%s%s %s".formatted(type, isVarargs ? "..." : "", name);
	}
}
