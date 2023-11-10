package com.github.maracas.roseau.model;

import java.util.List;

public class Executable extends Element {
	/**
	 * The return type of the executable.
	 */
	protected final String returnType;

	/**
	 * List of the executable's parameter types.
	 */
	protected final List<String> parametersTypes;

	/**
	 * List of referenced types for each parameter.
	 */
	protected final List<List<String>> parametersReferencedTypes;

	/**
	 * List of the executable's formal type parameters.
	 */
	protected final List<String> formalTypeParameters;

	protected final List<List<String>> formalTypeParamsBounds;

	/**
	 * The executable's signature.
	 */
	protected final Signature signature;

	/**
	 * List of exceptions thrown by the executable.
	 */
	protected final List<String> thrownExceptions;

	/**
	 * List of boolean values indicating varargs status for each parameter.
	 */
	protected final List<Boolean> parametersVarargsCheck;

	public Executable(String name, AccessModifier visibility, String returnType, List<String> referencedTypes, List<String> parametersTypes, List<List<String>> parametersReferencedTypes, List<String> formalTypeParameters, List<List<String>> formalTypeParamsBounds, List<Modifier> modifiers, Signature signature, List<String> thrownExceptions, List<Boolean> parametersVarargsCheck, String position) {
		super(name, visibility, modifiers, referencedTypes, position);
		this.returnType = returnType;
		this.parametersTypes = parametersTypes;
		this.parametersReferencedTypes = parametersReferencedTypes;
		this.formalTypeParameters = formalTypeParameters;
		this.formalTypeParamsBounds = formalTypeParamsBounds;
		this.signature = signature;
		this.thrownExceptions = thrownExceptions;
		this.parametersVarargsCheck = parametersVarargsCheck;
	}

	public boolean hasSameSignature(Executable other) {
		return signature.equals(other.signature);
	}

	/**
	 * Retrieves the return data type of the executable.
	 *
	 * @return executable's return data type
	 */
	public String getReturnType() {
		return returnType;
	}

	/**
	 * Retrieves the list of parameter data types of the executable.
	 *
	 * @return List of parameter data types
	 */
	public List<String> getParametersTypes() {
		return parametersTypes;
	}

	/**
	 * Retrieves the list of referenced types for each parameter of the executable.
	 *
	 * @return Lists of referenced types for parameters
	 */
	public List<List<String>> getParametersReferencedTypes() {
		return parametersReferencedTypes;
	}

	/**
	 * Retrieves the executable's formal type parameters.
	 *
	 * @return List of formal type parameters
	 */
	public List<String> getFormalTypeParameters() {
		return formalTypeParameters;
	}

	/**
	 * Retrieves a list of lists containing the formal type parameters' bounds.
	 *
	 * @return formal type parameters bounds
	 */
	public List<List<String>> getFormalTypeParamsBounds() {
		return formalTypeParamsBounds;
	}

	/**
	 * Retrieves the signature of the executable.
	 *
	 * @return The executable's signature
	 */
	public Signature getSignature() {
		return signature;
	}

	/**
	 * Retrieves the list of exceptions thrown by the executable.
	 *
	 * @return List of exceptions thrown by the executable
	 */
	public List<String> getThrownExceptions() {
		return thrownExceptions;
	}

	/**
	 * Retrieves the list of boolean values indicating varargs status for each parameter.
	 *
	 * @return List of varargs status for parameters
	 */
	public List<Boolean> getParametersVarargsCheck() {
		return parametersVarargsCheck;
	}
}
