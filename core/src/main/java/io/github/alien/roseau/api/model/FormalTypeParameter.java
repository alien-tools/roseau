package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;

/**
 * A formal type parameter (or type variable) declares an identifier used as a type (e.g. {@code <T extends String>}).
 *
 * @param name   the name of the formal type parameter
 * @param bounds an optional set of bounds implemented by the formal type parameter, or {@link TypeReference#OBJECT}
 */
public record FormalTypeParameter(
	String name,
	List<ITypeReference> bounds
) {
	public FormalTypeParameter {
		Preconditions.checkNotNull(name);
		Preconditions.checkNotNull(bounds);
		bounds = bounds.isEmpty() ? List.of(TypeReference.OBJECT) : List.copyOf(bounds);
	}

	@Override
	public String toString() {
		return String.format("%s extends %s", name, bounds.stream().map(ITypeReference::getQualifiedName).toList());
	}
}
