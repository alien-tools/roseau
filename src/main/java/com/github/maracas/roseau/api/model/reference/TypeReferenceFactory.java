package com.github.maracas.roseau.api.model.reference;

import com.github.maracas.roseau.api.model.TypeDecl;

import java.util.List;

public interface TypeReferenceFactory {
	<T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName);
	PrimitiveTypeReference createPrimitiveTypeReference(String name);
	ArrayTypeReference createArrayTypeReference(ITypeReference componentType);
	TypeParameterReference createTypeParameterReference(String qualifiedName, List<ITypeReference> bounds);
}
