package com.github.maracas.roseau.model;

import spoon.reflect.declaration.CtMethod;

public record MethodDeclaration(
	VisibilityKind visibility,
	String simpleName,
	String signature,
	String returnType,
	boolean isAbstract,
	boolean isFinal,
	boolean isStatic
) {
	public static MethodDeclaration of(CtMethod<?> method) {
		return new MethodDeclaration(
			VisibilityKind.of(method.getVisibility()),
			method.getSimpleName(),
			method.getSignature(),
			method.getType().getQualifiedName(),
			method.isAbstract(),
			method.isFinal(),
			method.isStatic()
		);
	}

	@Override
	public String toString() {
		return signature;
	}
}
