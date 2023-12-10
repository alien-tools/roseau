package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

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
		return hasSignature(other.getSimpleName(),
			other.getParameters().stream().map(ParameterDecl::type).toList(),
			!other.getParameters().isEmpty() && other.getParameters().getLast().isVarargs());
	}

	boolean hasSignature(String simpleName, List<? extends ITypeReference> parameterTypes, boolean varargs) {
		if (!getSimpleName().equals(simpleName))
			return false;

		if (parameters.size() != parameterTypes.size())
			return false;

		if (varargs && (parameters.isEmpty() || !parameters.getLast().isVarargs()))
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
	 * We assume that input source code compiles, so we won't have two methods
	 * with same name and parameters but different return types.
	 * Executable do not overload themselves.
	 */
	public boolean isOverloading(ExecutableDecl other) {
		return getSimpleName().equals(other.getSimpleName())
			&& !hasSameSignature(other)
			&& containingType.isSameHierarchy(other.getContainingType());
	}

	/**
	 * Executables override themselves
	 */
	public boolean isOverriding(ExecutableDecl other) {
		return hasSameSignature(other)
			&& containingType.isSubtypeOf(other.getContainingType());
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
