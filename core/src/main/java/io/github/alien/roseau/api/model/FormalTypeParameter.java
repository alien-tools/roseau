package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;

public record FormalTypeParameter(
	String name,
	List<ITypeReference> bounds
) implements DeepCopyable<FormalTypeParameter> {
	public FormalTypeParameter {
		Objects.requireNonNull(name);
		Objects.requireNonNull(bounds);
		if (bounds.isEmpty())
			bounds = List.of(TypeReference.OBJECT);
	}

	@Override
	public String toString() {
		return String.format("%s extends %s", name, bounds.stream().map(ITypeReference::getQualifiedName).toList());
	}

	@Override
	public FormalTypeParameter deepCopy() {
		return new FormalTypeParameter(name, ITypeReference.deepCopy(bounds));
	}
}
