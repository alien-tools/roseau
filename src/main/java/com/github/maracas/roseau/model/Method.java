package com.github.maracas.roseau.model;

import java.util.List;

/**
 * Represents a method declaration within a Java type.
 * This class extends the {@link Executable} class and complements it with method-specific information
 */
public class Method extends Executable {
	/**
	 * A flag indicating whether the method is a default method.
	 */
	private final boolean isDefault;

	public Method(String name, AccessModifier visibility, String returnType, List<String> referencedTypes, List<String> parametersTypes, List<List<String>> parametersReferencedTypes, List<String> formalTypeParameters, List<List<String>> formalTypeParamsBounds, List<Modifier> modifiers, Signature signature, List<String> thrownExceptions, List<Boolean> parametersVarargsCheck, boolean isDefault, String position) {
		super(name, visibility, returnType, referencedTypes, parametersTypes, parametersReferencedTypes, formalTypeParameters, formalTypeParamsBounds, modifiers, signature, thrownExceptions, parametersVarargsCheck, position);
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
		return "Method Name: " + getName() + "\n" +
			"Return Type: " + getReturnType() + "\n" +
			"Parameter Types: " + getParametersTypes() + "\n" +
			"Visibility: " + getVisibility() + "\n" +
			"Modifiers: " + getModifiers() + "\n" +
			"Exceptions: " + getThrownExceptions() + "\n" +
			"Position: " + getPosition() + "\n\n";
	}
}
