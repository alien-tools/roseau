package com.github.maracas.roseau.api.model.reference;

public record TypeParameterReference(String qualifiedName) implements ITypeReference {
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
