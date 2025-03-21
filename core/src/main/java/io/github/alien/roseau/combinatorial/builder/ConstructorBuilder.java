package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.ConstructorDecl;

public final class ConstructorBuilder extends ExecutableBuilder {
	public ConstructorDecl make() {
		return new ConstructorDecl(qualifiedName, visibility, modifiers, annotations, location,
				containingType, type, parameters, formalTypeParameters, thrownExceptions);
	}
}
