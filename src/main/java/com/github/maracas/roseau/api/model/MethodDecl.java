package com.github.maracas.roseau.api.model;

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

	private final boolean isAbstract;

	public MethodDecl(String qualifiedName, AccessModifier visibility, boolean isExported, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType, TypeReference<TypeDecl> returnType, List<ParameterDecl> parameters, List<FormalTypeParameter> formalTypeParameters, List<TypeReference<ClassDecl>> thrownExceptions, boolean isDefault, boolean isAbstract) {
		super(qualifiedName, visibility, isExported, modifiers, location, containingType, returnType, parameters, formalTypeParameters, thrownExceptions);
		this.isDefault = isDefault;
		this.isAbstract = isAbstract;
	}

	/**
	 * Checks if the method is a default method.
	 *
	 * @return True if the method is a default method, false otherwise
	 */
	public boolean isDefault() {
		return isDefault;
	}

	public boolean isAbstract() {
		return isAbstract;
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
			""".formatted(qualifiedName, visibility, modifiers, location);
	}
}
