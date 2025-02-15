package com.github.maracas.roseau.api.model.reference;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <? extends A> and <?> are upper bounds while <? super A> isn't
 * Wildcards have >= 1 upper bounds (java.lang.Object always included) or == 1 lower bound
 */
public record WildcardTypeReference(List<ITypeReference> bounds, boolean upper) implements ITypeReference {
	public WildcardTypeReference {
		if (bounds == null || bounds.isEmpty()) {
			throw new IllegalArgumentException("Wildcards must have at least one bound (Object included)");
		}
		if (!upper && bounds.size() > 1) {
			throw new IllegalArgumentException("Wildcards cannot have multiple lower bounds");
		}
	}

	@Override
	public String getQualifiedName() {
		return toString();
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		if (other instanceof WildcardTypeReference wtr) {
			if (wtr.isUnbounded()) {
				// Always subtype of unbounded wildcard
				return true;
			}

			if (upper()) {
				// Upper bounds can be made weaker
				return wtr.upper() && hasStricterBoundsThan(wtr);
			} else {
				// Changing the (one) lower bound to a subtype is fine
				return !wtr.upper() && wtr.bounds().getFirst().isSubtypeOf(bounds().getFirst());
			}
		}

		return false;
	}

	private boolean isUnbounded() {
		return upper() && bounds().size() == 1 && bounds.getFirst().equals(TypeReference.OBJECT);
	}

	/**
	 * Checks whether these bounds are stricter than the bounds of another wildcard
	 */
	private boolean hasStricterBoundsThan(WildcardTypeReference other) {
		return other.bounds().stream()
			.allMatch(otherBound -> bounds().stream().anyMatch(thisBound -> thisBound.isSubtypeOf(otherBound)));
	}

	@Override
	public String toString() {
		return "? %s %s".formatted(upper ? "extends" : "super",
			bounds.stream().map(Object::toString).collect(Collectors.joining("&")));
	}
}
