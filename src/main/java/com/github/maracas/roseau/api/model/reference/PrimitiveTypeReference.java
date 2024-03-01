package com.github.maracas.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record PrimitiveTypeReference(String name) implements ITypeReference {
	@JsonIgnore
	@Override
	public String getQualifiedName() {
		return name;
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		return false;
	}

	@Override
	public String toString() {
		return name;
	}
}
