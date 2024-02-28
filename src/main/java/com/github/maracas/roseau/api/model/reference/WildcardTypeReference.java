package com.github.maracas.roseau.api.model.reference;

import java.util.List;

public record WildcardTypeReference(List<ITypeReference> bounds, boolean upper) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return "?";
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		return false;
	}

	public boolean isUnbounded() {
		return bounds().size() == 1 && bounds().getFirst().getQualifiedName().equals("java.lang.Object");
	}
}
