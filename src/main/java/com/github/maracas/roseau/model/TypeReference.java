package com.github.maracas.roseau.model;

public final class TypeReference {
	public static final TypeReference NULL = new TypeReference("<null>");
	private final String qualifiedName;
	private TypeDecl actualType;

	public TypeReference(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}

	public String getQualifiedName() {
		return qualifiedName;
	}

	public TypeDecl getActualType() {
		return actualType;
	}

	@Override
	public String toString() {
		return qualifiedName;
	}
}
