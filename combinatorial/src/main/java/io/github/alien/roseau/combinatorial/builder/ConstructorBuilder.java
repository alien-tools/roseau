package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.ConstructorDecl;

import java.util.Set;

public final class ConstructorBuilder extends ExecutableBuilder {
	public ConstructorDecl make() {
		var parameters = this.parameters.stream().map(ParameterBuilder::make).toList();

		return new ConstructorDecl(qualifiedName, visibility, modifiers, Set.copyOf(annotations), location,
			containingType, type, parameters, formalTypeParameters, Set.copyOf(thrownExceptions));
	}

	public static ConstructorBuilder from(ConstructorDecl constructorDecl) {
		var builder = new ConstructorBuilder();

		builder.mutateWithDecl(constructorDecl);

		return builder;
	}
}
