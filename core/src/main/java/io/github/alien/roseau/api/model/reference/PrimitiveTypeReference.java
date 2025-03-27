package io.github.alien.roseau.api.model.reference;

import com.google.common.base.Preconditions;

/**
 * A reference to a primitive type (e.g., {@code int}, {@code byte}).
 *
 * @param name the simple name of this primitive type
 */
public record PrimitiveTypeReference(
	String name
) implements ITypeReference {
	public PrimitiveTypeReference {
		Preconditions.checkNotNull(name);
	}

	@Override
	public String getQualifiedName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public PrimitiveTypeReference deepCopy() {
		return this;
	}
}
