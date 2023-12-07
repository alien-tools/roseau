package com.github.maracas.roseau.api.model;

import spoon.reflect.factory.TypeFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class TypeParameterReference<T extends TypeDecl> extends TypeReference<T> {
	private final List<TypeReference<TypeDecl>> bounds;

	public TypeParameterReference(String qualifiedName, List<TypeReference<TypeDecl>> bounds, TypeFactory typeFactory) {
		super(qualifiedName, typeFactory);
		this.bounds = bounds;
	}

	@Override
	public Optional<T> getResolvedApiType() {
		return Optional.empty();
	}

	@Override
	public List<MethodDecl> getAllMethods() {
		return bounds.stream()
			.map(TypeReference::getAllMethods)
			.flatMap(Collection::stream)
			.toList();
	}

	@Override
	public List<FieldDecl> getAllFields() {
		return bounds.stream()
			.map(TypeReference::getAllFields)
			.flatMap(Collection::stream)
			.toList();
	}

	@Override
	public Optional<FieldDecl> findField(String name) {
		return super.findField(name);
	}

	@Override
	public List<TypeReference<InterfaceDecl>> getAllImplementedInterfaces() {
		return super.getAllImplementedInterfaces();
	}

	@Override
	public List<TypeReference<InterfaceDecl>> getImplementedInterfaces() {
		return super.getImplementedInterfaces();
	}

	@Override
	public List<FormalTypeParameter> getFormalTypeParameters() {
		return super.getFormalTypeParameters();
	}

	@Override
	public List<FieldDecl> getFields() {
		return super.getFields();
	}

	@Override
	public Optional<MethodDecl> findMethod(String name, List<TypeReference<TypeDecl>> parameterTypes) {
		return super.findMethod(name, parameterTypes);
	}

	@Override
	public List<MethodDecl> getMethods() {
		return super.getMethods();
	}
}
