package com.github.maracas.roseau.api.model.reference;

import java.util.Objects;

public record TypeParameterReference(String qualifiedName) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		return Objects.equals(qualifiedName, other.getQualifiedName());
	}

	@Override
	public String toString() {
		return qualifiedName;
	}
}
