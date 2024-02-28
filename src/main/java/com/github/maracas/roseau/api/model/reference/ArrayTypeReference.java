package com.github.maracas.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ArrayTypeReference(ITypeReference componentType) implements ITypeReference {
	@JsonIgnore
	@Override
	public String getQualifiedName() {
		return componentType().getQualifiedName() + "[]";
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		return false;
	}

	@Override
	public String toString() {
		return getQualifiedName();
	}
}
