package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An abstract executable is either a {@link MethodDecl} or a {@link ConstructorDecl}.
 * Executables may declare a list of parameters, formal type parameters, and potentially thrown exceptions.
 */
public abstract sealed class ExecutableDecl extends TypeMemberDecl permits MethodDecl, ConstructorDecl {
	protected final List<ParameterDecl> parameters;

	protected final List<FormalTypeParameter> formalTypeParameters;

	// Thrown exceptions aren't necessarily TypeReference<ClassDecl>
	// e.g.: <X extends Throwable> m() throws X
	protected final List<ITypeReference> thrownExceptions;

	protected ExecutableDecl(String qualifiedName, AccessModifier visibility, EnumSet<Modifier> modifiers,
	                         List<Annotation> annotations, SourceLocation location,
	                         TypeReference<TypeDecl> containingType, ITypeReference type, List<ParameterDecl> parameters,
	                         List<FormalTypeParameter> formalTypeParameters,
	                         List<ITypeReference> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type);
		this.parameters = Objects.requireNonNull(parameters);
		this.formalTypeParameters = Objects.requireNonNull(formalTypeParameters);
		this.thrownExceptions = Objects.requireNonNull(thrownExceptions);
	}

	public boolean isMethod() {
		return false;
	}

	public boolean isConstructor() {
		return false;
	}

	/**
	 * Checks whether the given ExecutableDecl has the same signature as the current instance.
	 *
	 * @param other The ExecutableDecl to compare the signature with
	 * @return true if they have the same signature, false otherwise
	 */
	public boolean hasSameErasure(ExecutableDecl other) {
		return equals(other) || Objects.equals(getErasure(), other.getErasure());
	}

	// §8.4.2
	public String getSignature() {
		return "%s(%s)".formatted(simpleName,
			parameters.stream()
				.map(p -> String.format("%s%s", p.type(), p.isVarargs() ? "[]" : ""))
				.collect(Collectors.joining(","))
		);
	}

	public String getErasure() {
		return "%s(%s)".formatted(simpleName,
			parameters.stream()
				.map(p -> String.format("%s%s", getErasure(p.type()).getQualifiedName(), p.isVarargs() ? "[]" : ""))
				.collect(Collectors.joining(","))
		);
	}

	public ITypeReference getErasure(ITypeReference type) {
		return switch (type) {
			case WildcardTypeReference wtr -> wtr.bounds().getFirst();
			case TypeParameterReference tpr ->
				resolveTypeParameter(tpr).map(t -> t.bounds().getFirst()).orElse(TypeReference.OBJECT);
			default -> type;
		};
	}

	public Optional<FormalTypeParameter> resolveTypeParameter(TypeParameterReference tpr) {
		var resolved = formalTypeParameters.stream()
			.filter(ftp -> ftp.name().equals(tpr.getQualifiedName()))
			.findFirst();

		return resolved.or(() -> containingType.getResolvedApiType().flatMap(t -> t.resolveTypeParameter(tpr)));
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
			&& !hasSameErasure(other)
			&& containingType.isSameHierarchy(other.getContainingType());
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

	public List<ITypeReference> getThrownExceptions() {
		return Collections.unmodifiableList(thrownExceptions);
	}

	public List<ITypeReference> getThrownCheckedExceptions() {
		return thrownExceptions.stream()
			.filter(e -> e.isSubtypeOf(TypeReference.EXCEPTION) && !e.isSubtypeOf(TypeReference.RUNTIME_EXCEPTION))
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
