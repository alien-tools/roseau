package io.github.alien.roseau.api.model.reference;

import io.github.alien.roseau.api.utils.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A wildcard type reference used as part of formal type parameters declarations.
 * <br>
 * {@code <? extends A>} and {@code <?>} are upper bounds while {@code <? super A>} is a lower bound. Wildcards have at
 * least one upper bounds ({@link java.lang.Object} always included) or exactly one lower bound.
 */
public record WildcardTypeReference(List<ITypeReference> bounds, boolean upper) implements ITypeReference {
	/**
	 * Creates a new wildcard type reference
	 *
	 * @param bounds the wildcard's bounds
	 * @param upper whether these are lower or upper bounds
	 * @throws IllegalArgumentException if no bounds is supplied, or if there are more than one lower bound
	 */
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
	public String getPrettyQualifiedName() {
		return StringUtils.splitSpecialCharsAndCapitalize(getQualifiedName());
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

	@Override
	public WildcardTypeReference deepCopy() {
		return new WildcardTypeReference(ITypeReference.deepCopy(bounds), upper);
	}
}
