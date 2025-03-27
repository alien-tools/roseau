package io.github.alien.roseau.api.model.reference;

import com.google.common.base.Preconditions;

/**
 * A reference to an array type (e.g., {@code String[]}).
 *
 * @param componentType the type of this array's component
 * @param dimension     the array's dimension (e.g., 2 for {@code String[][]})
 */
public record ArrayTypeReference(
	ITypeReference componentType,
	int dimension
) implements ITypeReference {
	/**
	 * Creates a new reference to an array type
	 *
	 * @param componentType the type of this array's component
	 * @param dimension     the array's dimension
	 * @throws IllegalArgumentException if dimension &lt; 1 or componentType is null
	 */
	public ArrayTypeReference {
		Preconditions.checkNotNull(componentType);
		Preconditions.checkArgument(dimension >= 1, "array dimension < 1");
	}

	@Override
	public String getQualifiedName() {
		return componentType().getQualifiedName() + "[]".repeat(dimension);
	}

	@Override
	public String toString() {
		return getQualifiedName();
	}
}
