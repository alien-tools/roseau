package com.github.maracas.roseau.model;

import spoon.reflect.declaration.CtType;

import java.util.List;

public record TypeDeclaration(
	VisibilityKind visibility,
	String qualifiedName,
	TypeKind kind,
	boolean isAbstract,
	boolean isFinal,
	boolean isCheckedException,
	List<ConstructorDeclaration> constructors,
	List<MethodDeclaration> methods,
	List<FieldDeclaration> fields
) {
	public TypeDeclaration(CtType<?> type, List<ConstructorDeclaration> constructors, List<MethodDeclaration> methods, List<FieldDeclaration> fields) {
		this(
			VisibilityKind.of(type.getVisibility()),
			type.getQualifiedName(),
			TypeKind.of(type),
			type.isAbstract(),
			type.isFinal(),
			type.isSubtypeOf(type.getFactory().createCtTypeReference(Exception.class)) &&
				!type.isSubtypeOf(type.getFactory().createCtTypeReference(RuntimeException.class)),
			constructors,
			methods,
		  fields
		);
	}

	@Override
	public String toString() {
		return qualifiedName;
	}
}
