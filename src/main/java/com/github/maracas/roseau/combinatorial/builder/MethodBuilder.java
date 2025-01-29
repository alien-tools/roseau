package com.github.maracas.roseau.combinatorial.builder;

import com.github.maracas.roseau.api.model.MethodDecl;

public final class MethodBuilder extends ExecutableBuilder {
	public MethodDecl make() {
		return new MethodDecl(qualifiedName, visibility, modifiers, annotations, location,
				containingType, type, parameters, formalTypeParameters, thrownExceptions);
	}
}
