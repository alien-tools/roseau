package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.api.model.TypeDecl;

import java.util.Optional;

/**
 * A type provider that can map fully qualified names to type declarations.
 */
public interface TypeProvider {
	/**
	 * Attempts to find the type declaration identified by a fully qualified name in this provider.
	 *
	 * @param qualifiedName the fully qualified name to look for
	 * @param type          the expected kind of type declaration
	 * @param <T>           the expected kind of type declaration
	 * @return an {@link Optional} indicating whether the corresponding type declaration was found
	 */
	<T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type);

	/**
	 * Attempts to find the type declaration identified by a fully qualified name in this provider.
	 *
	 * @param qualifiedName the fully qualified name to look for
	 * @return an {@link Optional} indicating whether the corresponding type declaration was found
	 */
	default Optional<TypeDecl> findType(String qualifiedName) {
		return findType(qualifiedName, TypeDecl.class);
	}
}
