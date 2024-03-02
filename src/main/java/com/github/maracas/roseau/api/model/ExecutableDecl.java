package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An abstract executable is either a {@link MethodDecl} or a {@link ConstructorDecl}.
 * Executables may declare a list of parameters, formal type parameters, and potentially thrown exceptions.
 */
public abstract sealed class ExecutableDecl extends TypeMemberDecl permits MethodDecl, ConstructorDecl {
	protected final List<ParameterDecl> parameters;

	protected final List<FormalTypeParameter> formalTypeParameters;

	protected final List<TypeReference<ClassDecl>> thrownExceptions;

	protected ExecutableDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers,
	                         List<Annotation> annotations, SourceLocation location,
	                         TypeReference<TypeDecl> containingType, ITypeReference type, List<ParameterDecl> parameters,
	                         List<FormalTypeParameter> formalTypeParameters,
	                         List<TypeReference<ClassDecl>> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type);
		this.parameters = Objects.requireNonNull(parameters);
		this.formalTypeParameters = Objects.requireNonNull(formalTypeParameters);
		this.thrownExceptions = Objects.requireNonNull(thrownExceptions);
	}

	/**
	 * Checks whether the given ExecutableDecl has the same signature as the current instance.
	 *
	 * @param other The ExecutableDecl to compare the signature with
	 * @return true if they have the same signature, false otherwise
	 */
	public boolean hasSameSignature(ExecutableDecl other) {
		return hasSignature(other.getSimpleName(),
			other.parameters.stream().map(ParameterDecl::type).toList(),
			other.isVarargs());
	}

	/**
	 * Checks whether this executable matches the given signature
	 *
	 * @param simpleName      The simple name of the method.
	 * @param parameterTypes  The list of parameter types of the method.
	 * @param varargs         Indicates whether the method is a varargs method.
	 * @return true if the method has the specified signature, false otherwise.
	 */
	public boolean hasSignature(String simpleName, List<? extends ITypeReference> parameterTypes, boolean varargs) {
		if (!Objects.equals(simpleName, getSimpleName()))
			return false;

		if (parameters.size() != parameterTypes.size())
			return false;

		if (varargs && !isVarargs())
			return false;

		for (int i = 0; i < parameterTypes.size(); i++) {
			ITypeReference otherType = parameterTypes.get(i);
			ITypeReference thisType = parameters.get(i).type();

			if (!otherType.getClass().equals(thisType.getClass()))
				return false;

			if (!Objects.equals(otherType.getQualifiedName(), thisType.getQualifiedName()))
				return false;
		}

		return true;
	}

	/**
	 * Checks whether the supplied executable and this instance are overloading each others.
	 * We assume that input source code compiles, so we won't have two methods
	 * with same name and parameters but different return types.
	 * An executable does not overload itself.
	 *
	 * @param other The other executable
	 * @return whether the two executables override each other
	 */
	public boolean isOverloading(ExecutableDecl other) {
		return Objects.equals(getSimpleName(), other.getSimpleName())
			&& !hasSameSignature(other)
			&& containingType.isSameHierarchy(other.getContainingType());
	}

	/**
	 * Checks whether the current instance overrides the supplied executable.
	 * An executable overrides itself.
	 *
	 * @param other The other executable
	 * @return whether this overrides other
	 */
	public boolean isOverriding(ExecutableDecl other) {
		return equals(other)
			|| (hasSameSignature(other) && containingType.isSubtypeOf(other.getContainingType()));
	}

	/**
	 * Checks whether this executable can be supplied with a variable number of arguments
	 */
	public boolean isVarargs() {
		return !parameters.isEmpty() && parameters.getLast().isVarargs();
	}

	public List<ParameterDecl> getParameters() {
		return Collections.unmodifiableList(parameters);
	}

	public List<FormalTypeParameter> getFormalTypeParameters() {
		return Collections.unmodifiableList(formalTypeParameters);
	}

	@Override
	public String getSimpleName() {
		return getQualifiedName().substring(getQualifiedName().lastIndexOf('.') + 1);
	}

	public List<TypeReference<ClassDecl>> getThrownExceptions() {
		return Collections.unmodifiableList(thrownExceptions);
	}

	public List<TypeReference<ClassDecl>> getThrownCheckedExceptions() {
		return thrownExceptions.stream()
			.filter(e -> e.getResolvedApiType().map(ClassDecl::isCheckedException).orElse(false))
			.toList();
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
