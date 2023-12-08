package com.github.maracas.roseau.api.model.reference;

import java.util.List;

public record TypeParameterReference(String qualifiedName, List<ITypeReference> bounds) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}
}
