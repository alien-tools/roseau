package com.github.maracas.roseau.api.model;

import java.util.List;

public final class ArrayTypeReference<T extends TypeDecl> extends TypeReference<T> {
	public ArrayTypeReference(String qualifiedName) {
		super(qualifiedName);
	}
}
