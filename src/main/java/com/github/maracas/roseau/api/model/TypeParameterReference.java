package com.github.maracas.roseau.api.model;

public final class TypeParameterReference<T extends TypeDecl> extends TypeReference<T> {
	public TypeParameterReference(String qualifiedName) {
		super(qualifiedName);
	}
}
