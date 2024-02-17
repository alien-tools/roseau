package com.github.maracas.roseau.api.model.reference;

import java.util.List;

public record WildcardTypeReference(List<ITypeReference> bounds, boolean upper) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return "?";
	}
}
