package com.github.maracas.roseau.api.model.reference;

import java.util.Objects;

public record ArrayTypeReference(ITypeReference componentType, int dimension) implements ITypeReference {
	public ArrayTypeReference {
		Objects.requireNonNull(componentType);
		if (dimension < 1)
			throw new IllegalArgumentException("array dimension < 1");
	}

	@Override
	public String getQualifiedName() {
		return componentType().getQualifiedName() + "[]".repeat(dimension);
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		return other instanceof ArrayTypeReference atr
			&& dimension == atr.dimension() && componentType.isSubtypeOf(atr.componentType());
	}

	@Override
	public String toString() {
		return getQualifiedName();
	}
}
