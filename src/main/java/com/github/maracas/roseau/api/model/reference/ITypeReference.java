package com.github.maracas.roseau.api.model.reference;

public sealed interface ITypeReference permits TypeReference, ArrayTypeReference, PrimitiveTypeReference, TypeParameterReference {
	String getQualifiedName();
}
