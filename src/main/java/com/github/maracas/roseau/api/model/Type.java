package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public interface Type {
	boolean isNested();

	boolean isClass();

	boolean isInterface();

	boolean isEnum();

	boolean isRecord();

	boolean isAnnotation();

	@JsonIgnore
	boolean isCheckedException();

	@JsonIgnore
	List<MethodDecl> getAllMethods();

	List<TypeReference<InterfaceDecl>> getSuperInterfaces();

	List<FormalTypeParameter> getFormalTypeParameters();

	List<FieldDecl> getFields();

	List<MethodDecl> getMethods();
}
