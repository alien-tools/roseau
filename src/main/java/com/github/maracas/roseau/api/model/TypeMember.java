package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

public interface TypeMember {
	TypeReference<TypeDecl> getContainingType();
	ITypeReference getType();
	String getSimpleName();
	boolean isStatic();
	boolean isFinal();
	boolean isPublic();
	boolean isProtected();
}
