package com.github.maracas.roseau.api.model;

import java.util.List;

/**
 * Represents a constructor declaration within a Java type.
 * This class extends the {@link ExecutableDecl} class and contains information about the constructor's parameters, return type, class, and more.
 */
public final class ConstructorDecl extends ExecutableDecl {
	public ConstructorDecl(String qualifiedName, AccessModifier visibility, boolean isExported, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType, TypeReference<TypeDecl> returnType, List<ParameterDecl> parameters, List<FormalTypeParameter> formalTypeParameters, List<TypeReference<ClassDecl>> thrownExceptions) {
		super(qualifiedName, visibility, isExported, modifiers, location, containingType, returnType, parameters, formalTypeParameters, thrownExceptions);
	}

	/**
	 * Generates a string representation of the ConstructorDeclaration.
	 *
	 * @return A formatted string containing the constructor's name, type, return type, parameter types,
	 * visibility, modifiers, exceptions, and position.
	 */
	@Override
	public String toString() {
		return "constructor %s [%s] (%s)".formatted(qualifiedName, visibility, location);
	}
}
