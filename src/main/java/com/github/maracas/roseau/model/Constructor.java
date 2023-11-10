package com.github.maracas.roseau.model;

import java.util.List;

/**
 * Represents a constructor declaration within a Java type.
 * This class extends the {@link Executable} class and contains information about the constructor's parameters, return type, class, and more.
 */
public class Constructor extends Executable {
	public Constructor(String name, AccessModifier visibility, String returnType, List<String> referencedTypes, List<String> parametersTypes, List<List<String>> parametersReferencedTypes, List<String> formalTypeParameters, List<List<String>> formalTypeParamsBounds, List<Modifier> modifiers, Signature signature, List<String> thrownExceptions, List<Boolean> parametersVarargsCheck, String position) {
		super(name, visibility, returnType, referencedTypes, parametersTypes, parametersReferencedTypes, formalTypeParameters, formalTypeParamsBounds, modifiers, signature, thrownExceptions, parametersVarargsCheck, position);
	}

	/**
	 * Generates a string representation of the ConstructorDeclaration.
	 *
	 * @return A formatted string containing the constructor's name, type, return type, parameter types,
	 * visibility, modifiers, exceptions, and position.
	 */
	@Override
	public String toString() {
		return "Constructor Name: " + getName() + "\n" +
			"Return Type: " + getReturnType() + "\n" +
			"Parameter Types: " + getParametersTypes() + "\n" +
			"Visibility: " + getVisibility() + "\n" +
			"Modifiers: " + getModifiers() + "\n" +
			"Exceptions: " + getThrownExceptions() + "\n" +
			"Position: " + getPosition() + "\n\n";
	}
}
