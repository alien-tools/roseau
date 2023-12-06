package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface TypeMember {
	TypeReference<TypeDecl> getType();
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
	@JsonIgnore
	boolean isNative();
	@JsonIgnore
	boolean isStrictFp();
}
