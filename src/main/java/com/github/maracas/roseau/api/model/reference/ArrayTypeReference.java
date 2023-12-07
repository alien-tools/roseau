package com.github.maracas.roseau.api.model.reference;

public record ArrayTypeReference(ITypeReference componentType) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return componentType().getQualifiedName() + "[]";
	}
}
