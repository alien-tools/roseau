package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a method declaration within a Java type.
 * This class extends the {@link ExecutableDecl} class and complements it with method-specific information
 */
public final class MethodDecl extends ExecutableDecl {
	public MethodDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, List<Annotation> annotations,
	                  SourceLocation location, TypeReference<TypeDecl> containingType, ITypeReference type, List<ParameterDecl> parameters,
	                  List<FormalTypeParameter> formalTypeParameters, List<TypeReference<ClassDecl>> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type, parameters,
			formalTypeParameters, thrownExceptions);
	}

	/**
	 * Checks if the method is a default method.
	 *
	 * @return True if the method is a default method, false otherwise
	 */
	public boolean isDefault() {
		return modifiers.contains(Modifier.DEFAULT);
	}

	public boolean isAbstract() {
		return modifiers.contains(Modifier.ABSTRACT);
	}

	public boolean isNative() {
		return modifiers.contains(Modifier.NATIVE);
	}

	public boolean isStrictFp() {
		return modifiers.contains(Modifier.STRICTFP);
	}

	/**
	 * Generates a string representation of the MethodDeclaration.
	 *
	 * @return A formatted string containing the method's qualifiedName, return type, parameter types,
	 * visibility, modifiers, type, exceptions, and position.
	 */
	@Override
	public String toString() {
		return "%s %s %s %s(%s)".formatted(
			modifiers.stream().map(Object::toString).collect(Collectors.joining(", ")), visibility, type, getSimpleName(),
			parameters.stream().map(Object::toString).collect(Collectors.joining(", ")));
	}
}
