package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;

/**
 * Represents a constructor declaration within a Java type. This class extends the {@link ExecutableDecl} class and
 * contains information about the constructor's parameters, return type, class, and more.
 */
public final class ConstructorDecl extends ExecutableDecl {
	@JsonCreator
	public ConstructorDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, List<Annotation> annotations,
	                       SourceLocation location, TypeReference<TypeDecl> containingType, ITypeReference type,
	                       List<ParameterDecl> parameters, List<FormalTypeParameter> formalTypeParameters,
	                       List<TypeReference<ClassDecl>> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type,
			parameters, formalTypeParameters, thrownExceptions);
	}

	/**
	 * Generates a string representation of the ConstructorDeclaration.
	 *
	 * @return A formatted string containing the constructor's qualifiedName, type, return type, parameter types,
	 * visibility, modifiers, exceptions, and position.
	 */
	@Override
	public String toString() {
		return "constructor %s [%s] (%s)".formatted(qualifiedName, visibility, location);
	}
}
