package com.github.maracas.roseau.api.model;

import spoon.reflect.factory.TypeFactory;

public final class TypeParameterReference<T extends TypeDecl> extends TypeReference<T> {
	public TypeParameterReference(String qualifiedName) {
		super(qualifiedName);
	}

	public TypeParameterReference(String qualifiedName, TypeFactory typeFactory) {
		super(qualifiedName, typeFactory);
	}
}
