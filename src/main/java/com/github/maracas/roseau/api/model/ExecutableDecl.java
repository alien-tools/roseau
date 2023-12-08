package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public abstract sealed class ExecutableDecl extends TypeMemberDecl permits MethodDecl, ConstructorDecl {
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

	protected ExecutableDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers,
	                         SourceLocation location, TypeReference<TypeDecl> containingType, ITypeReference type,
	                         List<ParameterDecl> parameters, List<FormalTypeParameter> formalTypeParameters,
	                         List<TypeReference<ClassDecl>> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, location, containingType, type);
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

	public boolean hasSignature(String name, List<ITypeReference> parameterTypes) {
		// FIXME: varargs + merge with above
		if (!getSimpleName().equals(name))
			return false;

		if (parameters.size() != parameterTypes.size())
			return false;

		for (int i = 0; i < parameterTypes.size(); i++) {
			ITypeReference otherParameter = parameterTypes.get(i);
			ITypeReference thisParameter = parameters.get(i).type();

			if (!otherParameter.equals(thisParameter))
				return false;
		}

		return true;
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

	@Override
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

	@JsonIgnore
	public boolean isNative() {
		return modifiers.contains(Modifier.NATIVE);
	}

	@JsonIgnore
	public boolean isStrictFp() {
		return modifiers.contains(Modifier.STRICTFP);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		ExecutableDecl other = (ExecutableDecl) o;
		return Objects.equals(parameters, other.parameters)
			&& Objects.equals(formalTypeParameters, other.formalTypeParameters)
			&& Objects.equals(thrownExceptions, other.thrownExceptions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), parameters, formalTypeParameters, thrownExceptions);
	}
}
