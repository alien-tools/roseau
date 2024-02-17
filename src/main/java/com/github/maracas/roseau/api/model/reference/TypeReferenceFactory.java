package com.github.maracas.roseau.api.model.reference;

import com.github.maracas.roseau.api.model.TypeDecl;

import java.util.Collections;
import java.util.List;

public interface TypeReferenceFactory {
	<T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName, List<ITypeReference> typeArguments);
	PrimitiveTypeReference createPrimitiveTypeReference(String name);
	ArrayTypeReference createArrayTypeReference(ITypeReference componentType);
	TypeParameterReference createTypeParameterReference(String qualifiedName);
	WildcardTypeReference createWildcardTypeReference(List<ITypeReference> bounds, boolean upper);

	default <T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName) {
		return createTypeReference(qualifiedName, Collections.emptyList());
	}
}
