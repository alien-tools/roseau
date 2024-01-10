package com.github.maracas.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.maracas.roseau.api.model.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.TypeDecl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TypeReference<T extends TypeDecl> implements ITypeReference {
	private final String qualifiedName;
	private final List<ITypeReference> typeArguments;
	@JsonIgnore
	private SpoonAPIFactory factory;
	@JsonIgnore
	private T resolvedApiType;

	@JsonCreator
	public TypeReference(String qualifiedName, List<ITypeReference> typeArguments) {
		this.qualifiedName = qualifiedName;
		this.typeArguments = typeArguments;
	}

	public TypeReference(String qualifiedName, List<ITypeReference> typeArguments, SpoonAPIFactory factory) {
		this(qualifiedName, typeArguments);
		this.factory = factory;
	}

	public SpoonAPIFactory getFactory() { return factory; }

	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	public List<ITypeReference> getTypeArguments() {
		return typeArguments;
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

		return Objects.equals(qualifiedName, other.qualifiedName) && Objects.equals(typeArguments, other.typeArguments);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(qualifiedName);
	}
}
