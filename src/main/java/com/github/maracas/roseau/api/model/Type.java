package com.github.maracas.roseau.api.model;

import java.util.List;
import java.util.Optional;

public interface Type {
	boolean isNested();

	boolean isClass();

	boolean isInterface();

	boolean isEnum();

	boolean isRecord();

	boolean isAnnotation();

	boolean isCheckedException();

	boolean isStatic();

	boolean isFinal();

	boolean isPublic();

	boolean isProtected();

	boolean isAbstract();

	List<TypeReference<InterfaceDecl>> getSuperInterfaces();

	List<FormalTypeParameter> getFormalTypeParameters();

	List<FieldDecl> getFields();

	List<MethodDecl> getMethods();

	List<MethodDecl> getAllMethods();

	Optional<FieldDecl> getField(String name);
}
