package com.github.maracas.roseau.model;

import java.util.List;

/**
 * Represents a method declaration within a Java type.
 * This class extends the {@link ExecutableDecl} class and complements it with method-specific information
 */
public final class MethodDecl extends ExecutableDecl {
	/**
	 * A flag indicating whether the method is a default method.
	 */
	private final boolean isDefault;

	public MethodDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, String position, TypeReference containingType, TypeReference returnType, List<ParameterDecl> parameters, List<FormalTypeParameter> formalTypeParameters, List<TypeReference> thrownExceptions, boolean isDefault) {
		super(qualifiedName, visibility, modifiers, position, containingType, returnType, parameters, formalTypeParameters, thrownExceptions);
		this.isDefault = isDefault;
	}

	/**
	 * Checks if the method is a default method.
	 *
	 * @return True if the method is a default method, false otherwise
	 */
	public boolean isDefault() {
		return isDefault;
	}

	/**
	 * Generates a string representation of the MethodDeclaration.
	 *
	 * @return A formatted string containing the method's name, return type, parameter types,
	 * visibility, modifiers, type, exceptions, and position.
	 */
	@Override
	public String toString() {
		return """
			Method %s [%s] [%s]
			  Position: %s
			""".formatted(qualifiedName, visibility, modifiers, position);
	}
}
