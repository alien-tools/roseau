package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.maracas.roseau.api.model.reference.ITypeReference;

public interface TypeMember {
	ITypeReference getType();
	@JsonIgnore
	String getSimpleName();
	@JsonIgnore
	boolean isStatic();
	@JsonIgnore
	boolean isPublic();
	@JsonIgnore
	boolean isProtected();
	@JsonIgnore
	boolean isFinal();
}
