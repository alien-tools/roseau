package com.github.maracas.roseau.api.model;

import spoon.reflect.factory.TypeFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class PrimitiveTypeReference<T extends TypeDecl> extends TypeReference<T> {
	public PrimitiveTypeReference(String qualifiedName, TypeFactory typeFactory) {
		super(qualifiedName, typeFactory);
	}

	@Override
	public Optional<T> getResolvedApiType() {
		return Optional.empty();
	}

	@Override
	public boolean isClass() {
		return false;
	}

	@Override
	public boolean isInterface() {
		return false;
	}

	@Override
	public boolean isEnum() {
		return false;
	}

	@Override
	public boolean isRecord() {
		return false;
	}

	@Override
	public boolean isAnnotation() {
		return false;
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}

	@Override
	public boolean isFinal() {
		return true;
	}

	@Override
	public boolean isEffectivelyFinal() {
		return true;
	}

	@Override
	public List<TypeReference<InterfaceDecl>> getImplementedInterfaces() {
		return Collections.emptyList();
	}

	@Override
	public List<FormalTypeParameter> getFormalTypeParameters() {
		return Collections.emptyList();
	}

	@Override
	public List<MethodDecl> getMethods() {
		return Collections.emptyList();
	}

	@Override
	public List<FieldDecl> getFields() {
		return Collections.emptyList();
	}

	@Override
	public Optional<MethodDecl> findMethod(String name, List<TypeReference<TypeDecl>> parameterTypes) {
		return Optional.empty();
	}

	@Override
	public Optional<FieldDecl> findField(String name) {
		return Optional.empty();
	}

	@Override
	public List<TypeReference<InterfaceDecl>> getAllImplementedInterfaces() {
		return Collections.emptyList();
	}

	@Override
	public List<MethodDecl> getAllMethods() {
		return Collections.emptyList();
	}

	@Override
	public List<FieldDecl> getAllFields() {
		return Collections.emptyList();
	}
}
