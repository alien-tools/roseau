package com.github.maracas.roseau.api.model.reference;

import java.util.Objects;

public record PrimitiveTypeReference(String qualifiedName) implements ITypeReference {
	public PrimitiveTypeReference {
		Objects.requireNonNull(qualifiedName);
	}

	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		return false;
	}

	@Override
	public String toString() {
		return qualifiedName;
	}
}
