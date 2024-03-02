package com.github.maracas.roseau.api.model.reference;

public record ArrayTypeReference(ITypeReference componentType) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return componentType().getQualifiedName() + "[]";
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		return componentType.isSubtypeOf(other);
	}

	@Override
	public String toString() {
		return getQualifiedName();
	}
}
