package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Optional;

/**
 * A type resolver that resolves {@link TypeReference} instances to their corresponding {@link TypeDecl}.
 */
public interface TypeResolver {
	/**
	 * Resolves the given type reference into its corresponding type declaration.
	 *
	 * @param reference the type reference to resolve
	 * @param <T>       the expected kind of type declaration
	 * @return an {@link Optional} indicating whether the reference was successfully resolved
	 */
	<T extends TypeDecl> Optional<T> resolve(TypeReference<T> reference);
}
