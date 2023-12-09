package com.github.maracas.roseau.api.model.reference;

import java.util.List;
import java.util.stream.Collectors;

public record TypeParameterReference(String qualifiedName, List<ITypeReference> bounds) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public String toString() {
		return "%s<%s>".formatted(qualifiedName,
			bounds.stream().map(Object::toString).collect(Collectors.joining(", ")));
	}
}
