package com.github.maracas.roseau.model;

import com.fasterxml.jackson.annotation.JsonValue;

public final class TypeReference {
	private final String qualifiedName;
	private TypeDecl actualType;

	public TypeReference(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}

	@JsonValue
	public String getQualifiedName() {
		return qualifiedName;
	}

	public TypeDecl getActualType() {
		return actualType;
	}

	public void setActualType(TypeDecl type) {
		this.actualType = type;
	}

	@Override
	public String toString() {
		return qualifiedName;
	}
}
