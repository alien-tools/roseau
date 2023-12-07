package com.github.maracas.roseau.api.model.reference;

public record PrimitiveTypeReference(String qualifiedName) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}
}
