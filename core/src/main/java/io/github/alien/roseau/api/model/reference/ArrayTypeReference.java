package io.github.alien.roseau.api.model.reference;

import io.github.alien.roseau.api.utils.StringUtils;

import java.util.Objects;

/**
 * A reference to an array type (e.g., {@code String[]}).
 *
 * @param componentType the type of this array's component
 * @param dimension     the array's dimension (e.g., 2 for {@code String[][]})
 */
public record ArrayTypeReference(ITypeReference componentType, int dimension) implements ITypeReference {
	/**
	 * Creates a new reference to an array type
	 *
	 * @param componentType the type of this array's component
	 * @param dimension     the array's dimension
	 * @throws IllegalArgumentException if dimension &lt; 1
	 */
	public ArrayTypeReference {
		Objects.requireNonNull(componentType);
		if (dimension < 1) {
			throw new IllegalArgumentException("array dimension < 1");
		}
	}

	@Override
	public String getQualifiedName() {
		return componentType().getQualifiedName() + "[]".repeat(dimension);
	}

	@Override
	public String getPrettyQualifiedName() {
		return StringUtils.splitSpecialCharsAndCapitalize(getQualifiedName() + "Array".repeat(dimension));
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		if (other instanceof ArrayTypeReference(ITypeReference otherType, int otherDimension)) {
			return dimension == otherDimension && componentType.isSubtypeOf(otherType);
		}

		return false;
	}

	@Override
	public String toString() {
		return getQualifiedName();
	}

	@Override
	public ArrayTypeReference deepCopy() {
		return new ArrayTypeReference(componentType.deepCopy(), dimension);
	}
}
