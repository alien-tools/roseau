package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.MethodDecl;

public final class MethodBuilder extends ExecutableBuilder {
	public MethodDecl make() {
		var parameters = this.parameters.stream().map(ParameterBuilder::make).toList();

		return new MethodDecl(qualifiedName, visibility, modifiers, annotations, location,
				containingType, type, parameters, formalTypeParameters, thrownExceptions);
	}

	public static MethodBuilder from(MethodDecl methodDecl) {
		var builder = new MethodBuilder();

		builder.mutateWithDecl(methodDecl);

		return builder;
	}
}
