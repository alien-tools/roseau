package com.github.maracas.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ArrayTypeReference(ITypeReference componentType) implements ITypeReference {
	@JsonIgnore
	@Override
	public String getQualifiedName() {
		return componentType().getQualifiedName() + "[]";
	}
}
