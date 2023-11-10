package com.github.maracas.roseau.model;

import java.util.List;

/**
 * Represents a constructor declaration within a Java type.
 * This class extends the {@link ExecutableDecl} class and contains information about the constructor's parameters, return type, class, and more.
 */
public final class ConstructorDecl extends ExecutableDecl {
	public ConstructorDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, String position, TypeReference containingType, TypeReference returnType, List<ParameterDecl> parameters, List<FormalTypeParameter> formalTypeParameters, List<TypeReference> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, position, containingType, returnType, parameters, formalTypeParameters, thrownExceptions);
	}

	/**
	 * Generates a string representation of the ConstructorDeclaration.
	 *
	 * @return A formatted string containing the constructor's name, type, return type, parameter types,
	 * visibility, modifiers, exceptions, and position.
	 */
	@Override
	public String toString() {
		return """
			Constructor %s [%s] [%s]
			  Position: %s
			""".formatted(qualifiedName, visibility, modifiers, position);
	}
}
