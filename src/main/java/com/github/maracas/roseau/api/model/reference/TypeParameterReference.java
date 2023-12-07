package com.github.maracas.roseau.api.model.reference;

import java.util.List;

public record TypeParameterReference(String name, List<ITypeReference> bounds) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return name;
	}
}
