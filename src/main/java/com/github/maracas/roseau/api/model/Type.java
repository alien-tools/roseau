package com.github.maracas.roseau.api.model;

import java.util.List;
import java.util.Optional;

public interface Type {
	// Type kinds
	boolean isClass();
	boolean isInterface();
	boolean isEnum();
	boolean isRecord();
	boolean isAnnotation();

	// Visibility
	boolean isPublic();
	boolean isProtected();
	boolean isPrivate();
	boolean isPackagePrivate();

	// Modifiers
	boolean isStatic();
	boolean isAbstract();
	boolean isFinal();
	boolean isSealed();
	// Others

	boolean isNested();
	boolean isCheckedException();
	boolean isEffectivelyFinal();

	// Navigation
	List<TypeReference<InterfaceDecl>> getImplementedInterfaces();
	List<FormalTypeParameter> getFormalTypeParameters();
	List<FieldDecl> getFields();
	List<MethodDecl> getMethods();
	Optional<FieldDecl> getField(String name);

	// Transitive navigations
	List<TypeReference<InterfaceDecl>> getAllImplementedInterfaces();
	List<MethodDecl> getAllMethods();
}
