package io.github.alien.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A named reference to another type (primitive, type parameter, wildcard, array, or type declaration).
 *
 * @see TypeReferenceFactory
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "refKind")
public sealed interface ITypeReference
	permits TypeReference, ArrayTypeReference, PrimitiveTypeReference, TypeParameterReference, WildcardTypeReference {
	/**
	 * The qualified name this reference points to
	 *
	 * @return the qualified name
	 */
	String getQualifiedName();
}
