package com.github.maracas.roseau.api.model.reference;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public record WildcardTypeReference(List<ITypeReference> bounds, boolean upper) implements ITypeReference {
	@Override
	public String getQualifiedName() {
		return toString();
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		if (other instanceof WildcardTypeReference wtr) {
			// Changing upper bounds to supertypes is fine
			if (upper())
				return wtr.upper() && areAllCompatible(bounds(), wtr.bounds());
			// Changing lower bounds to subtypes is fine
			else
				return !wtr.upper() && areAllCompatible(wtr.bounds(), bounds);
		}

		return false;
	}

	/**
	 * Checks whether every type in {@code types1} has a corresponding supertype in {@code types2}, so that
	 * a value that would conform to all {@code types1} would also conform to all {@code types2}
	 */
	private boolean areAllCompatible(Collection<ITypeReference> types1, Collection<ITypeReference> types2) {
		return types1.stream().allMatch(t1 -> types2.stream().anyMatch(t1::isSubtypeOf));
	}

	@Override
	public String toString() {
		return "? %s %s".formatted(upper ? "extends" : "super",
			bounds.stream().map(Object::toString).collect(Collectors.joining("&")));
	}
}
