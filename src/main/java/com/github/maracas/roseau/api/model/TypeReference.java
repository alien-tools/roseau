package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Objects;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TypeReference<T extends TypeDecl> implements Type {
	private final String qualifiedName;
	private T resolvedApiType;
	private CtTypeReference<?> foreignTypeReference;

	public TypeReference(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}

	@JsonValue
	public String getQualifiedName() {
		return qualifiedName;
	}

	public Optional<T> getResolvedApiType() {
		return Optional.ofNullable(resolvedApiType);
	}

	public Optional<CtTypeReference<?>> getForeignTypeReference() {
		return Optional.ofNullable(foreignTypeReference);
	}

	public void setResolvedApiType(T type) {
		this.resolvedApiType = type;
	}

	public void setForeignTypeReference(CtTypeReference<?> ref) {
		this.foreignTypeReference = ref;
	}

	@Override
	public String toString() {
		return qualifiedName;
	}

	@Override
	public boolean isNested() {
		if (resolvedApiType != null)
			return resolvedApiType.isNested();
		else if (foreignTypeReference != null)
			return !foreignTypeReference.getTypeDeclaration().isTopLevel();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isClass() {
		if (resolvedApiType != null)
			return resolvedApiType.isClass();
		else if (foreignTypeReference != null)
			return foreignTypeReference.isClass();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isInterface() {
		if (resolvedApiType != null)
			return resolvedApiType.isInterface();
		else if (foreignTypeReference != null)
			return foreignTypeReference.isInterface();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isEnum() {
		if (resolvedApiType != null)
			return resolvedApiType.isEnum();
		else if (foreignTypeReference != null)
			return foreignTypeReference.isEnum();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isRecord() {
		if (resolvedApiType != null)
			return resolvedApiType.isRecord();
		else if (foreignTypeReference != null)
			return foreignTypeReference.getTypeDeclaration() instanceof CtRecord;
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isAnnotation() {
		if (resolvedApiType != null)
			return resolvedApiType.isAnnotation();
		else if (foreignTypeReference != null)
			return foreignTypeReference.isAnnotationType();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isCheckedException() {
		if (resolvedApiType != null)
			return resolvedApiType.isCheckedException();
		else if (foreignTypeReference != null) {
			CtType<?> t = foreignTypeReference.getTypeDeclaration();

			return t.isSubtypeOf(t.getFactory().Type().createReference(Exception.class))
				&& !t.isSubtypeOf(t.getFactory().Type().createReference(RuntimeException.class));
		}
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isStatic() {
		if (resolvedApiType != null)
			return resolvedApiType.isStatic();
		else if (foreignTypeReference != null)
			return foreignTypeReference.getTypeDeclaration().isStatic();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isFinal() {
		if (resolvedApiType != null)
			return resolvedApiType.isFinal();
		else if (foreignTypeReference != null)
			return foreignTypeReference.getTypeDeclaration().isFinal();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isSealed() {
		if (resolvedApiType != null)
			return resolvedApiType.isSealed();
		else if (foreignTypeReference != null)
			return foreignTypeReference.getTypeDeclaration().hasModifier(ModifierKind.SEALED);
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isEffectivelyFinal() {
		if (resolvedApiType != null)
			return resolvedApiType.isEffectivelyFinal();
		else if (foreignTypeReference != null)
			return foreignTypeReference.getTypeDeclaration().hasModifier(ModifierKind.FINAL) || foreignTypeReference.getTypeDeclaration().hasModifier(ModifierKind.SEALED);
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isPublic() {
		if (resolvedApiType != null)
			return resolvedApiType.isPublic();
		else if (foreignTypeReference != null)
			return foreignTypeReference.getTypeDeclaration().isPublic();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isProtected() {
		if (resolvedApiType != null)
			return resolvedApiType.isProtected();
		else if (foreignTypeReference != null)
			return foreignTypeReference.getTypeDeclaration().isProtected();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isPrivate() {
		if (resolvedApiType != null)
			return resolvedApiType.isPrivate();
		else if (foreignTypeReference != null)
			return foreignTypeReference.getTypeDeclaration().isPrivate();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isPackagePrivate() {
		if (resolvedApiType != null)
			return resolvedApiType.isPackagePrivate();
		else if (foreignTypeReference != null)
			return !foreignTypeReference.getTypeDeclaration().isPublic() && !foreignTypeReference.getTypeDeclaration().isProtected() && !foreignTypeReference.getTypeDeclaration().isPrivate();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public boolean isAbstract() {
		if (resolvedApiType != null)
			return resolvedApiType.isAbstract();
		else if (foreignTypeReference != null)
			return foreignTypeReference.getTypeDeclaration().isAbstract();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public List<MethodDecl> getAllMethods() {
		if (resolvedApiType != null)
			return resolvedApiType.getAllMethods();
		else if (foreignTypeReference != null)
			return Collections.emptyList();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public Optional<FieldDecl> getField(String name) {
		if (resolvedApiType != null)
			return resolvedApiType.getField(name);
		else if (foreignTypeReference != null)
			return Optional.empty();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public List<TypeReference<InterfaceDecl>> getAllImplementedInterfaces() {
		if (resolvedApiType != null)
			return resolvedApiType.getAllImplementedInterfaces();
		else if (foreignTypeReference != null)
			return Collections.emptyList();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public List<TypeReference<InterfaceDecl>> getImplementedInterfaces() {
		if (resolvedApiType != null)
			return resolvedApiType.getImplementedInterfaces();
		else if (foreignTypeReference != null)
			return Collections.emptyList();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public List<FormalTypeParameter> getFormalTypeParameters() {
		if (resolvedApiType != null)
			return resolvedApiType.getFormalTypeParameters();
		else if (foreignTypeReference != null)
			return Collections.emptyList();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public List<FieldDecl> getFields() {
		if (resolvedApiType != null)
			return resolvedApiType.getFields();
		else if (foreignTypeReference != null)
			return Collections.emptyList();
		throw new RuntimeException("Unresolved reference");
	}

	@Override
	public List<MethodDecl> getMethods() {
		if (resolvedApiType != null)
			return resolvedApiType.getMethods();
		else if (foreignTypeReference != null)
			return Collections.emptyList();
		throw new RuntimeException("Unresolved reference");
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
