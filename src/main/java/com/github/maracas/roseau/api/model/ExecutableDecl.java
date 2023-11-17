package com.github.maracas.roseau.api.model;

import java.util.List;

public abstract sealed class ExecutableDecl extends Symbol permits MethodDecl, ConstructorDecl {
	/**
	 * The return type of the executable.
	 */
	protected final TypeReference<TypeDecl> returnType;

	/**
	 * List of the executable's parameter types.
	 */
	protected final List<ParameterDecl> parameters;

	/**
	 * List of the executable's formal type parameters.
	 */
	protected final List<FormalTypeParameter> formalTypeParameters;

	/**
	 * List of exceptions thrown by the executable.
	 */
	protected final List<TypeReference<ClassDecl>> thrownExceptions;

	protected ExecutableDecl(String qualifiedName, AccessModifier visibility, boolean isExported, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType, TypeReference<TypeDecl> returnType, List<ParameterDecl> parameters, List<FormalTypeParameter> formalTypeParameters, List<TypeReference<ClassDecl>> thrownExceptions) {
		super(qualifiedName, visibility, isExported, modifiers, location, containingType);
		this.returnType = returnType;
		this.parameters = parameters;
		this.formalTypeParameters = formalTypeParameters;
		this.thrownExceptions = thrownExceptions;
	}

	public boolean hasSameSignature(ExecutableDecl other) {
		if (!other.getSimpleName().equals(getSimpleName()))
			return false;

		if (other.parameters.size() != parameters.size())
			return false;

		for (int i = 0; i < other.parameters.size(); i++) {
			ParameterDecl otherParameter = other.parameters.get(i);
			ParameterDecl thisParameter = parameters.get(i);

			if (otherParameter.isVarargs() != thisParameter.isVarargs())
				return false;
			if (otherParameter.type() != thisParameter.type())
				return false;
		}

		return true;
	}

	/**
	 * Retrieves the return data type of the executable.
	 *
	 * @return executable's return data type
	 */
	public TypeReference<TypeDecl> getReturnType() {
		return returnType;
	}

	/**
	 * Retrieves the list of parameters
	 *
	 * @return List of parameters
	 */
	public List<ParameterDecl> getParameters() {
		return parameters;
	}

	/**
	 * Retrieves the executable's formal type parameters.
	 *
	 * @return List of formal type parameters
	 */
	public List<FormalTypeParameter> getFormalTypeParameters() {
		return formalTypeParameters;
	}

	public String getSimpleName() {
		return getQualifiedName().substring(getQualifiedName().lastIndexOf('.') + 1);
	}

	/**
	 * Retrieves the list of exceptions thrown by the executable.
	 *
	 * @return List of exceptions thrown by the executable
	 */
	public List<TypeReference<ClassDecl>> getThrownExceptions() {
		return thrownExceptions;
	}
}
