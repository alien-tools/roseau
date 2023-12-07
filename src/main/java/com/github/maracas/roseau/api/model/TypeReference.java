package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Objects;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtTypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TypeReference<T extends TypeDecl> implements Type {
	private final String qualifiedName;
	private TypeFactory typeFactory;
	private T resolvedApiType;

	public TypeReference(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}

	public TypeReference(String qualifiedName, TypeFactory typeFactory) {
		this.qualifiedName = qualifiedName;
		this.typeFactory = typeFactory;
	}

	public void setTypeFactory(TypeFactory typeFactory) {
		this.typeFactory = typeFactory;
	}

	@JsonValue
	public String getQualifiedName() {
		return qualifiedName;
	}

	public Optional<T> getResolvedApiType() {
		if (resolvedApiType == null && typeFactory != null) {
			CtTypeReference<?> ref = typeFactory.createReference(qualifiedName);

			if (ref.getTypeDeclaration() != null)
				resolvedApiType = (T) new SpoonAPIFactory(typeFactory).convertCtType(ref.getTypeDeclaration());
		}

		return Optional.ofNullable(resolvedApiType);
	}

	public void setResolvedApiType(T type) {
		this.resolvedApiType = type;
	}

	@Override
	public String toString() {
		return qualifiedName;
	}

	@Override
	public boolean isNested() {
		return getResolvedApiType().map(TypeDecl::isNested).orElse(false);
	}

	@Override
	public boolean isClass() {
		return getResolvedApiType().map(TypeDecl::isClass).orElse(false);
	}

	@Override
	public boolean isInterface() {
		return getResolvedApiType().map(TypeDecl::isInterface).orElse(false);
	}

	@Override
	public boolean isEnum() {
		return getResolvedApiType().map(TypeDecl::isEnum).orElse(false);
	}

	@Override
	public boolean isRecord() {
		return getResolvedApiType().map(TypeDecl::isRecord).orElse(false);
	}

	@Override
	public boolean isAnnotation() {
		return getResolvedApiType().map(TypeDecl::isAnnotation).orElse(false);
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}

	@Override
	public boolean isCheckedException() {
		return getResolvedApiType().map(TypeDecl::isCheckedException).orElse(false);
	}

	@Override
	public boolean isStatic() {
		return getResolvedApiType().map(TypeDecl::isStatic).orElse(false);
	}

	@Override
	public boolean isFinal() {
		return getResolvedApiType().map(TypeDecl::isFinal).orElse(false);
	}

	@Override
	public boolean isSealed() {
		return getResolvedApiType().map(TypeDecl::isSealed).orElse(false);
	}

	@Override
	public boolean isEffectivelyFinal() {
		return getResolvedApiType().map(TypeDecl::isEffectivelyFinal).orElse(false);
	}

	@Override
	public boolean isPublic() {
		return getResolvedApiType().map(TypeDecl::isPublic).orElse(false);
	}

	@Override
	public boolean isProtected() {
		return getResolvedApiType().map(TypeDecl::isProtected).orElse(false);
	}

	@Override
	public boolean isPrivate() {
		return getResolvedApiType().map(TypeDecl::isPrivate).orElse(false);
	}

	@Override
	public boolean isPackagePrivate() {
		return getResolvedApiType().map(TypeDecl::isPackagePrivate).orElse(false);
	}

	@Override
	public boolean isExported() {
		return getResolvedApiType().map(TypeDecl::isExported).orElse(false);
	}

	@Override
	public boolean isAbstract() {
		return getResolvedApiType().map(TypeDecl::isAbstract).orElse(false);
	}

	@Override
	public List<MethodDecl> getAllMethods() {
		return getResolvedApiType().map(TypeDecl::getAllMethods).orElse(Collections.emptyList());
	}

	@Override
	public List<FieldDecl> getAllFields() {
		return getResolvedApiType().map(TypeDecl::getAllFields).orElse(Collections.emptyList());
	}

	@Override
	public Optional<FieldDecl> findField(String name) {
		return getResolvedApiType().flatMap(t -> t.findField(name));
	}

	@Override
	public List<TypeReference<InterfaceDecl>> getAllImplementedInterfaces() {
		return getResolvedApiType().map(TypeDecl::getAllImplementedInterfaces).orElse(Collections.emptyList());
	}

	@Override
	public List<TypeReference<InterfaceDecl>> getImplementedInterfaces() {
		return getResolvedApiType().map(TypeDecl::getImplementedInterfaces).orElse(Collections.emptyList());
	}

	@Override
	public List<FormalTypeParameter> getFormalTypeParameters() {
		return getResolvedApiType().map(TypeDecl::getFormalTypeParameters).orElse(Collections.emptyList());
	}

	@Override
	public List<FieldDecl> getFields() {
		return getResolvedApiType().map(TypeDecl::getFields).orElse(Collections.emptyList());
	}

	@Override
	public Optional<MethodDecl> findMethod(String name, List<TypeReference<TypeDecl>> parameterTypes) {
		return getResolvedApiType().flatMap(t -> t.findMethod(name, parameterTypes));
	}

	@Override
	public List<MethodDecl> getMethods() {
		return getResolvedApiType().map(TypeDecl::getMethods).orElse(Collections.emptyList());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TypeReference<?> that = (TypeReference<?>) o;
		return Objects.equal(qualifiedName, that.qualifiedName);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(qualifiedName);
	}
}
