package io.github.alien.roseau.api.model.reference;

import java.util.Objects;

/**
 * A reference to a formal type parameter or type variable (e.g., {@code A} referring {@code <A>}).
 *
 * @param qualifiedName the simple name of the pointed type variable
 */
public record TypeParameterReference(String qualifiedName) implements ITypeReference {
	public TypeParameterReference {
		Objects.requireNonNull(qualifiedName);
	}

	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		// FIXME
		return Objects.equals(qualifiedName, other.getQualifiedName());
	}

	@Override
	public String toString() {
		return qualifiedName;
	}

	@Override
	public TypeParameterReference deepCopy() {
		return this;
	}
}
