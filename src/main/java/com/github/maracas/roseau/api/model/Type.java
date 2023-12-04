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
	boolean isPackagePrivate();

	// Modifiers
	boolean isStatic();
	boolean isAbstract();
	boolean isFinal();
	boolean isSealed();
	boolean isEffectivelyFinal();

	// Others
	boolean isNested();
	boolean isCheckedException();

	// Navigation
	List<TypeReference<InterfaceDecl>> getSuperInterfaces();
	List<FormalTypeParameter> getFormalTypeParameters();
	List<FieldDecl> getFields();
	List<MethodDecl> getMethods();
	List<MethodDecl> getAllMethods();
	Optional<FieldDecl> getField(String name);
}
