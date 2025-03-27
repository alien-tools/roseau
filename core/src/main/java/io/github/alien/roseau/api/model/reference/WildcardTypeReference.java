package io.github.alien.roseau.api.model.reference;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A wildcard type reference used as part of formal type parameters declarations.
 * <br>
 * {@code <? extends A>} and {@code <?>} are upper bounds while {@code <? super A>} is a lower bound. Wildcards have at
 * least one upper bounds ({@link java.lang.Object} always included) or exactly one lower bound.
 */
public record WildcardTypeReference(
	List<ITypeReference> bounds,
	boolean upper
) implements ITypeReference {
	/**
	 * Creates a new wildcard type reference
	 *
	 * @param bounds the wildcard's bounds
	 * @param upper  whether these are lower or upper bounds
	 * @throws IllegalArgumentException if no bounds is supplied, or if there are more than one lower bound
	 */
	public WildcardTypeReference {
		Preconditions.checkArgument(bounds != null && !bounds.isEmpty(),
			"Wildcards must have at least one bound (java.lang.Object included)");
		Preconditions.checkArgument(upper || bounds.size() == 1,
			"Wildcards cannot have multiple lower bounds");
		bounds = List.copyOf(bounds);
	}

	@Override
	public String getQualifiedName() {
		return toString();
	}

	public boolean isUnbounded() {
		return upper() && bounds().size() == 1 && bounds.getFirst().equals(TypeReference.OBJECT);
	}

	@Override
	public String toString() {
		return "? %s %s".formatted(upper ? "extends" : "super",
			bounds.stream().map(ITypeReference::toString).collect(Collectors.joining("&")));
	}
}
