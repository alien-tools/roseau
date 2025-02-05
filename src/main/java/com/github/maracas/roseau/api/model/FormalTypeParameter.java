package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;

import java.util.List;
import java.util.Objects;

public record FormalTypeParameter(
	String name,
	List<ITypeReference> bounds
) {
	public FormalTypeParameter {
		Objects.requireNonNull(name);
		Objects.requireNonNull(bounds);
	}

	@Override
	public String toString() {
		return String.format("%s extends %s", name, bounds.stream().map(ITypeReference::getQualifiedName).toList());
	}
}
