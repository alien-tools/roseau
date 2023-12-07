package com.github.maracas.roseau.api.model;

import spoon.reflect.factory.TypeFactory;

public final class ArrayTypeReference<T extends TypeDecl> extends TypeReference<T> {
	public ArrayTypeReference(String qualifiedName, TypeFactory typeFactory) {
		super(qualifiedName, typeFactory);
	}
}
