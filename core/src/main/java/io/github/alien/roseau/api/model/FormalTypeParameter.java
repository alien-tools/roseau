package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;

/**
 * A formal type parameter (or type variable) declares an identifier used as a type (e.g. {@code <T extends String>}).
 *
 * @param name   the name of the formal type parameter
 * @param bounds an optional set of bounds implemented by the formal type parameter, or {@link TypeReference#OBJECT}
 */
public record FormalTypeParameter(
	String name,
	List<ITypeReference> bounds
) implements DeepCopyable<FormalTypeParameter> {
	public FormalTypeParameter {
		Objects.requireNonNull(name);
		Objects.requireNonNull(bounds);
		if (bounds.isEmpty()) {
			bounds = List.of(TypeReference.OBJECT);
		}
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
