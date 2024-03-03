package com.github.maracas.roseau.api.model.reference;

import java.util.List;
import java.util.stream.Collectors;

public record WildcardTypeReference(List<ITypeReference> bounds, boolean upper) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return toString();
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		return false; // FIXME
	}

	public boolean isUnbounded() {
		return upper() && bounds().size() == 1 && bounds.getFirst().equals(TypeReference.OBJECT);
	}

	@Override
	public String toString() {
		return "? %s %s".formatted(upper ? "extends" : "super",
			bounds.stream().map(Object::toString).collect(Collectors.joining("&")));
	}
}
