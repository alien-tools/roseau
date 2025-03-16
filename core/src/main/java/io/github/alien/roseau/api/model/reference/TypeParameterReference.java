package io.github.alien.roseau.api.model.reference;

import java.util.Objects;

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
