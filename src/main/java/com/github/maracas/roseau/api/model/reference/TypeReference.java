package com.github.maracas.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.TypeDecl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class TypeReference<T extends TypeDecl> implements ITypeReference {
	private final String qualifiedName;
	private final List<ITypeReference> typeArguments;
	@JsonIgnore
	private SpoonAPIFactory factory;
	@JsonIgnore
	private T resolvedApiType;

	@JsonCreator
	public TypeReference(String qualifiedName, List<ITypeReference> typeArguments) {
		this.qualifiedName = Objects.requireNonNull(qualifiedName);
		this.typeArguments = Objects.requireNonNull(typeArguments);
	}

	public TypeReference(String qualifiedName, List<ITypeReference> typeArguments, SpoonAPIFactory factory) {
		this(qualifiedName, typeArguments);
		this.factory = Objects.requireNonNull(factory);
	}

	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	public List<ITypeReference> getTypeArguments() {
		return typeArguments;
	}

	public SpoonAPIFactory getFactory() { return factory; }

	public void setFactory(SpoonAPIFactory factory) {
		this.factory = Objects.requireNonNull(factory);
	}

	public Optional<T> getResolvedApiType() {
		if (resolvedApiType == null && factory != null)
			// Safe as long as we don't have two types with same FQN of different kinds (e.g. class vs interface)
			resolvedApiType = (T) factory.convertCtType(qualifiedName);

		return Optional.ofNullable(resolvedApiType);
	}

	public void setResolvedApiType(T type) {
		resolvedApiType = Objects.requireNonNull(type);
	}

	public boolean isSubtypeOf(ITypeReference other) {
		return equals(other)
			|| "java.lang.Object".equals(other.getQualifiedName())
			|| getResolvedApiType().map(t -> t.getAllSuperTypes().anyMatch(sup -> sup.equals(other))).orElse(false);
	}

	public boolean isSameHierarchy(TypeReference<T> other) {
		return isSubtypeOf(other) || other.isSubtypeOf(this);
	}

	public boolean isExported() {
		return getResolvedApiType().map(TypeDecl::isExported).orElse(false);
	}

	public boolean isEffectivelyFinal() {
		return getResolvedApiType().map(TypeDecl::isEffectivelyFinal).orElse(false);
	}

	@Override
	public String toString() {
		return "%s<%s>".formatted(qualifiedName,
			typeArguments.stream().map(Object::toString).collect(Collectors.joining(",")));
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
		return Objects.hash(qualifiedName, typeArguments);
	}
}
