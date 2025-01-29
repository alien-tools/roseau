package com.github.maracas.roseau.combinatorial.builder;

import com.github.maracas.roseau.api.model.ConstructorDecl;

public final class ConstructorBuilder extends ExecutableBuilder {
	public ConstructorDecl make() {
		return new ConstructorDecl(qualifiedName, visibility, modifiers, annotations, location,
				containingType, type, parameters, formalTypeParameters, thrownExceptions);
	}
}
