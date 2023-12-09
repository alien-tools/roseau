package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

public interface TypeMember {
	TypeReference<TypeDecl> getContainingType();
	ITypeReference getType();
	@JsonIgnore
	String getSimpleName();
	@JsonIgnore
	boolean isStatic();
	@JsonIgnore
	boolean isFinal();
	@JsonIgnore
	boolean isPublic();
	@JsonIgnore
	boolean isProtected();
}
