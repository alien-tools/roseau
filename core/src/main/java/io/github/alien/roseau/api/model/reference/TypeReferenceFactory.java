package io.github.alien.roseau.api.model.reference;

import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.List;

/**
 * An abstract factory of {@link ITypeReference} instances. All references within an {@link LibraryTypes} should be
 * created using the same factory. Implementations can return the same instance when the same reference is created
 * multiple times.
 *
 * @see CachingTypeReferenceFactory
 */
public interface TypeReferenceFactory {
	/**
	 * Creates a new type reference towards a {@link TypeDecl} of kind {@code <T>}.
	 *
	 * @param qualifiedName the fully qualified name of the {@link TypeDecl} the reference points to
	 * @param typeArguments the type arguments of the new reference (e.g., {@code List<String>}
	 * @param <T>           the kind of type declaration the new reference points to
	 * @return the new type reference
	 * @see TypeReference<T>
	 */
	<T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName, List<ITypeReference> typeArguments);

	/**
	 * Creates a new reference towards a primitive type reference.
	 *
	 * @param simpleName the simple name of the primitive type
	 * @return the new type reference
	 * @see PrimitiveTypeReference
	 */
	PrimitiveTypeReference createPrimitiveTypeReference(String simpleName);

	/**
	 * Creates a new reference towards an array type.
	 *
	 * @param componentType the type of the array's components
	 * @param dimension     the dimension of the array (e.g., 2 for {@code String[][]})
	 * @return the new type reference
	 * @see ArrayTypeReference
	 */
	ArrayTypeReference createArrayTypeReference(ITypeReference componentType, int dimension);

	/**
	 * Creates a new reference towards a formal type parameter.
	 *
	 * @param simpleName the simple name of the pointed type parameter
	 * @return the new type reference
	 * @see TypeParameterReference
	 */
	TypeParameterReference createTypeParameterReference(String simpleName);

	/**
	 * Creates a new wildcard type reference (e.g., {@code <? extends A>} or {@code <? super B}).
	 *
	 * @param bounds the wildcard's bounds
	 * @param upper  true if these are upper bounds ({@code ? extends}), false otherwise
	 * @return the new type reference
	 * @see WildcardTypeReference
	 */
	WildcardTypeReference createWildcardTypeReference(List<ITypeReference> bounds, boolean upper);

	/**
	 * Creates a new unparameterized type reference towards a {@link TypeDecl} of kind {@code <T>}.
	 *
	 * @param qualifiedName the fully qualified name of the {@link TypeDecl} the reference points to
	 * @param <T>           the kind of type declaration the new reference points to
	 * @return the new type reference
	 * @throws NullPointerException if {@code name} is null
	 * @see TypeReference<T>
	 */
	default <T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName) {
		return createTypeReference(qualifiedName, List.of());
	}
}
