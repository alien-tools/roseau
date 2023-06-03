package com.github.maracas.roseau.model;

import spoon.reflect.declaration.CtConstructor;

public record ConstructorDeclaration(
	VisibilityKind visibility,
	String signature
) {
	public static ConstructorDeclaration of(CtConstructor<?> cons) {
		return new ConstructorDeclaration(
			VisibilityKind.of(cons.getVisibility()),
			cons.getSignature()
		);
	}

	@Override
	public String toString() {
		return signature;
	}
}
