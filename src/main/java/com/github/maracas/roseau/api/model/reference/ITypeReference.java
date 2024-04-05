package com.github.maracas.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "refKind")
public sealed interface ITypeReference
	permits TypeReference, ArrayTypeReference, PrimitiveTypeReference, TypeParameterReference, WildcardTypeReference {
	String getQualifiedName();

	boolean isSubtypeOf(ITypeReference other);
}
