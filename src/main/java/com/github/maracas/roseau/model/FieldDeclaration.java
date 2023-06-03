package com.github.maracas.roseau.model;

import spoon.reflect.declaration.CtField;

public record FieldDeclaration(
	VisibilityKind visibility,
	String name,
	boolean isFinal,
	boolean isStatic
) {
	public static FieldDeclaration of(CtField<?> field) {
		return new FieldDeclaration(
			VisibilityKind.of(field.getVisibility()),
			field.getSimpleName(),
			field.isFinal(),
			field.isStatic()
		);
	}

	@Override
	public String toString() {
		return name;
	}
}
