package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.MethodDecl;

public final class MethodBuilder extends ExecutableBuilder {
	public MethodDecl make() {
		return new MethodDecl(qualifiedName, visibility, modifiers, annotations, location,
				containingType, type, parameters, formalTypeParameters, thrownExceptions);
	}
}
