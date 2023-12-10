package com.github.maracas.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonValue;
import com.github.maracas.roseau.api.model.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.google.common.base.Objects;

import java.util.Optional;

public final class TypeReference<T extends TypeDecl> implements ITypeReference {
	private final String qualifiedName;
	private SpoonAPIFactory factory;
	private T resolvedApiType;

	public TypeReference(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}

	public TypeReference(String qualifiedName, SpoonAPIFactory factory) {
		this.qualifiedName = qualifiedName;
		this.factory = factory;
	}

	@JsonValue
	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	public void setFactory(SpoonAPIFactory factory) {
		this.factory = factory;
	}

	public Optional<T> getResolvedApiType() {
		if (resolvedApiType == null && factory != null)
			resolvedApiType = (T) factory.convertCtType(qualifiedName);

		return Optional.ofNullable(resolvedApiType);
	}

	public void setResolvedApiType(T type) {
		resolvedApiType = type;
	}

	public boolean isSubtypeOf(TypeReference<T> other) {
		return equals(other) || getResolvedApiType().map(t -> t.getAllSuperTypes().contains(other)).orElse(false);
	}

	public boolean isSameHierarchy(TypeReference<T> other) {
		return isSubtypeOf(other) || other.isSubtypeOf(this);
	}

	@Override
	public String toString() {
		return qualifiedName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TypeReference<?> other = (TypeReference<?>) o;
		return Objects.equal(qualifiedName, other.qualifiedName);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(qualifiedName);
	}
}
