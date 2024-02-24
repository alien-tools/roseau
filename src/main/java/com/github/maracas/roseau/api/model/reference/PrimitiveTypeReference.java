package com.github.maracas.roseau.api.model.reference;

public record PrimitiveTypeReference(String name) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
