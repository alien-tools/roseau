package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class TypeReference<T extends TypeDecl> implements Type {
	private final String qualifiedName;
	private T actualType;

	public TypeReference(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}

	@JsonValue
	public String getQualifiedName() {
		return qualifiedName;
	}

	public Optional<T> getActualType() {
		return actualType != null
			? Optional.of(actualType)
			: Optional.empty();
	}

	public void setActualType(T type) {
		this.actualType = type;
	}

	@Override
	public String toString() {
		return qualifiedName;
	}

	@Override
	public boolean isNested() {
		return actualType != null && actualType.isNested();
	}

	@Override
	public boolean isClass() {
		return actualType != null && actualType.isClass();
	}

	@Override
	public boolean isInterface() {
		return actualType != null && actualType.isInterface();
	}

	@Override
	public boolean isEnum() {
		return actualType != null && actualType.isEnum();
	}

	@Override
	public boolean isRecord() {
		return actualType != null && actualType.isRecord();
	}

	@Override
	public boolean isAnnotation() {
		return actualType != null && actualType.isAnnotation();
	}

	@Override
	public boolean isCheckedException() {
		return actualType != null && actualType.isCheckedException();
	}

	@Override
	public boolean isStatic() {
		return actualType != null && actualType.isStatic();
	}

	@Override
	public boolean isFinal() {
		return actualType != null && actualType.isFinal();
	}

	@Override
	public boolean isPublic() {
		return actualType != null && actualType.isPublic();
	}

	@Override
	public boolean isProtected() {
		return actualType != null && actualType.isProtected();
	}

	@Override
	public boolean isAbstract() {
		return actualType != null && actualType.isAbstract();
	}

	@Override
	public List<MethodDecl> getAllMethods() {
		return actualType != null
			? actualType.getAllMethods()
			: Collections.emptyList();
	}

	@Override
	public Optional<FieldDecl> getField(String name) {
		return actualType != null
			? actualType.getField(name)
			: Optional.empty();
	}

	@Override
	public List<TypeReference<InterfaceDecl>> getSuperInterfaces() {
		return actualType != null
			? actualType.getSuperInterfaces()
			: Collections.emptyList();
	}

	@Override
	public List<FormalTypeParameter> getFormalTypeParameters() {
		return actualType != null
			? actualType.getFormalTypeParameters()
			: Collections.emptyList();
	}

	@Override
	public List<FieldDecl> getFields() {
		return actualType != null
			? actualType.getFields()
			: Collections.emptyList();
	}

	@Override
	public List<MethodDecl> getMethods() {
		return actualType != null
			? actualType.getMethods()
			: Collections.emptyList();
	}
}
