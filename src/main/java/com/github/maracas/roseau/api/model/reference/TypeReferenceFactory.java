package com.github.maracas.roseau.api.model.reference;

import com.github.maracas.roseau.api.model.TypeDecl;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface TypeReferenceFactory {
	<T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName, List<ITypeReference> typeArguments);
	PrimitiveTypeReference createPrimitiveTypeReference(String qualifiedName);
	ArrayTypeReference createArrayTypeReference(ITypeReference componentType, int dimension);
	TypeParameterReference createTypeParameterReference(String qualifiedName);
	WildcardTypeReference createWildcardTypeReference(List<ITypeReference> bounds, boolean upper);

	default <T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName) {
		return createTypeReference(Objects.requireNonNull(qualifiedName), Collections.emptyList());
	}
}
