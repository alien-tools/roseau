package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Optional;

public interface Type {
	// Type kinds
	@JsonIgnore
	boolean isClass();
	@JsonIgnore
	boolean isInterface();
	@JsonIgnore
	boolean isEnum();
	@JsonIgnore
	boolean isRecord();
	@JsonIgnore
	boolean isAnnotation();
	@JsonIgnore
	boolean isPrimitive();

	// Visibility
	@JsonIgnore
	boolean isPublic();
	@JsonIgnore
	boolean isProtected();
	@JsonIgnore
	boolean isPrivate();
	@JsonIgnore
	boolean isPackagePrivate();
	@JsonIgnore
	boolean isExported();

	// Modifiers
	@JsonIgnore
	boolean isStatic();
	@JsonIgnore
	boolean isAbstract();
	@JsonIgnore
	boolean isFinal();
	@JsonIgnore
	boolean isSealed();

	// Others
	@JsonIgnore
	boolean isNested();
	@JsonIgnore
	boolean isCheckedException();
	@JsonIgnore
	boolean isEffectivelyFinal();

	// Navigation
	List<TypeReference<InterfaceDecl>> getImplementedInterfaces();
	List<FormalTypeParameter> getFormalTypeParameters();
	List<MethodDecl> getMethods();
	List<FieldDecl> getFields();

	// Search
	Optional<MethodDecl> findMethod(String name, List<TypeReference<TypeDecl>> parameterTypes);
	Optional<FieldDecl> findField(String name);

	// Transitive navigations
	@JsonIgnore
	List<TypeReference<InterfaceDecl>> getAllImplementedInterfaces();
	@JsonIgnore
	List<MethodDecl> getAllMethods();
	@JsonIgnore
	List<FieldDecl> getAllFields();
}
