package com.github.maracas.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonValue;
import com.github.maracas.roseau.api.model.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.google.common.base.Objects;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtTypeReference;

import java.util.Optional;

public final class TypeReference<T extends TypeDecl> implements ITypeReference {
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

	@JsonValue
	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	public void setTypeFactory(TypeFactory typeFactory) {
		this.typeFactory = typeFactory;
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
