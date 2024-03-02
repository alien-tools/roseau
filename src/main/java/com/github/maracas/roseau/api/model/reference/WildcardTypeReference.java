package com.github.maracas.roseau.api.model.reference;

import java.util.List;

public record WildcardTypeReference(List<ITypeReference> bounds, boolean upper) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return "?";
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		return false; // FIXME
	}

	public boolean isUnbounded() {
		return upper() && bounds().size() == 1 && "java.lang.Object".equals(bounds().getFirst().getQualifiedName());
	}
}
