package com.github.maracas.roseau.api.model.reference;

import com.github.maracas.roseau.api.model.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.TypeDecl;

import java.util.List;

public class SpoonTypeReferenceFactory implements TypeReferenceFactory {
	private final SpoonAPIFactory apiFactory;

	public SpoonTypeReferenceFactory(SpoonAPIFactory apiFactory) {
		this.apiFactory = apiFactory;
	}

	@Override
	public <T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName, List<ITypeReference> typeArguments) {
		return new TypeReference<>(qualifiedName, typeArguments, apiFactory);
	}

	@Override
	public PrimitiveTypeReference createPrimitiveTypeReference(String name) {
		return new PrimitiveTypeReference(name);
	}

	@Override
	public ArrayTypeReference createArrayTypeReference(ITypeReference componentType) {
		return componentType != null ? new ArrayTypeReference(componentType) : null;
	}

	@Override
	public TypeParameterReference createTypeParameterReference(String qualifiedName) {
		return new TypeParameterReference(qualifiedName);
	}
}
