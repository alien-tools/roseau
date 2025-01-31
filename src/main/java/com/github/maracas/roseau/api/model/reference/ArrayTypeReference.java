package com.github.maracas.roseau.api.model.reference;

import com.github.maracas.roseau.api.utils.StringUtils;

import java.util.Objects;

public record ArrayTypeReference(ITypeReference componentType, int dimension) implements ITypeReference {
	public ArrayTypeReference {
		Objects.requireNonNull(componentType);
		if (dimension < 1)
			throw new IllegalArgumentException("array dimension < 1");
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
		if (other instanceof ArrayTypeReference(ITypeReference otherType, int otherDimension))
			return dimension == otherDimension && componentType.isSubtypeOf(otherType);

		return false;
	}

	@Override
	public String toString() {
		return getQualifiedName();
	}
}
