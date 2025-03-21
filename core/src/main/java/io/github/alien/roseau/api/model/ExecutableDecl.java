package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An abstract executable is either a {@link MethodDecl} or a {@link ConstructorDecl}. Executables can declare a list of
 * parameters, formal type parameters, and potentially thrown exceptions.
 */
public abstract sealed class ExecutableDecl extends TypeMemberDecl permits MethodDecl, ConstructorDecl {
	protected final List<ParameterDecl> parameters;
	protected final List<FormalTypeParameter> formalTypeParameters;
	// Thrown exceptions aren't necessarily TypeReference<ClassDecl>
	// e.g.: <X extends Throwable> m() throws X
	protected final List<ITypeReference> thrownExceptions;

	protected ExecutableDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                         List<Annotation> annotations, SourceLocation location,
	                         TypeReference<TypeDecl> containingType, ITypeReference type, List<ParameterDecl> parameters,
	                         List<FormalTypeParameter> formalTypeParameters,
	                         List<ITypeReference> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type);
		this.parameters = Objects.requireNonNull(parameters);
		this.formalTypeParameters = Objects.requireNonNull(formalTypeParameters);
		this.thrownExceptions = Objects.requireNonNull(thrownExceptions);
	}

	/**
	 * Checks whether this executable is a method.
	 *
	 * @return true if this executable is a method and not a constructor.
	 */
	public boolean isMethod() {
		return false;
	}

	/**
	 * Checks whether this executable is a constructor.
	 *
	 * @return true if this executable is a constructor and not a method.
	 */
	public boolean isConstructor() {
		return false;
	}

	/**
	 * The unqualified signature of an executable as specified in JLS §8.4.2. Types in a signature are not erased.
	 *
	 * @return the executable's signature
	 */
	public String getSignature() {
		var sb = new StringBuilder();
		sb.append(simpleName);
		sb.append("(");
		for (int i = 0; i < parameters.size(); i++) {
			var p = parameters.get(i);
			sb.append(p.type());
			if (p.isVarargs()) {
				sb.append("[]");
			}
			if (i < parameters.size() - 1) {
				sb.append(",");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * The unqualified erasure of the signature of an executable as specified in JLS §4.6. Types are replaced by their
	 * erasure.
	 *
	 * @return the executable's signature
	 */
	public String getErasure() {
		var sb = new StringBuilder();
		sb.append(simpleName);
		sb.append("(");
		for (int i = 0; i < parameters.size(); i++) {
			var p = parameters.get(i);
			sb.append(getErasedType(p.type()).getQualifiedName());
			if (p.isVarargs()) {
				sb.append("[]");
			}
			if (i < parameters.size() - 1) {
				sb.append(",");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	private ITypeReference getErasedType(ITypeReference ref) {
		return switch (ref) {
			case TypeParameterReference tpr -> resolveTypeParameterBound(tpr);
			case ArrayTypeReference(var t, var dimension) -> new ArrayTypeReference(getErasedType(t), dimension);
			default -> ref;
		};
	}

	/**
	 * Checks whether the supplied {@link ExecutableDecl} has the same erasure as the current instance.
	 *
	 * @param other The {@link ExecutableDecl} to compare erasure with
	 * @return true if they have the same erasure, false otherwise
	 * @throws NullPointerException if {@code other} is null
	 */
	public boolean hasSameErasure(ExecutableDecl other) {
		return Objects.equals(getErasure(), Objects.requireNonNull(other).getErasure());
	}

	/**
	 * Attempts to resolve the {@link FormalTypeParameter} declared by this executable and pointed by the supplied
	 * {@link TypeParameterReference}.
	 *
	 * @param typeParameterReference the {@link TypeParameterReference} to resolve
	 * @return an {@link Optional} indicating whether the referenced {@link FormalTypeParameter} was found
	 * @throws NullPointerException if {@code typeParameterReference} is null
	 * @see #getFormalTypeParametersInScope()
	 */
	public Optional<FormalTypeParameter> resolveTypeParameter(TypeParameterReference typeParameterReference) {
		return getFormalTypeParametersInScope().stream()
			.filter(ftp -> ftp.name().equals(Objects.requireNonNull(typeParameterReference).getQualifiedName()))
			.findFirst();
	}

	/**
	 * Resolves the left-most bound of the supplied {@link TypeParameterReference}. Bounds are resolved recursively (e.g.
	 * {@code <A extends B>}) within the current {@link #getFormalTypeParametersInScope()}.
	 *
	 * @param typeParameterReference the {@link TypeParameterReference} to resolve
	 * @return the resolved bound, or {@link TypeReference#OBJECT}
	 * @throws NullPointerException if {@code typeParameterReference} is null
	 * @see #resolveTypeParameter(TypeParameterReference)
	 */
	public ITypeReference resolveTypeParameterBound(TypeParameterReference typeParameterReference) {
		var resolved = resolveTypeParameter(Objects.requireNonNull(typeParameterReference));

		if (resolved.isPresent()) {
			var bound = resolved.get().bounds().getFirst();
			if (bound instanceof TypeParameterReference tpr) {
				return resolveTypeParameterBound(tpr);
			} else {
				return bound;
			}
		} else {
			return TypeReference.OBJECT;
		}
	}

	/**
	 * Returns the list of formal type parameters in this executable's scope, including (recursively) from its containing
	 * types.
	 *
	 * @return all {@link FormalTypeParameter} in this executable's scope
	 */
	public List<FormalTypeParameter> getFormalTypeParametersInScope() {
		return Stream.concat(formalTypeParameters.stream(),
				containingType.getResolvedApiType()
					.map(TypeDecl::getFormalTypeParametersInScope)
					.orElse(Collections.emptyList())
					.stream())
			.toList();
	}

	/**
	 * Checks whether the supplied executable and this instance are overloading each others. Assuming that the current
	 * {@link API} is consistent (the source compiles), there should not be two methods with the same erasure but
	 * different return types, so an executable overloads another if it has the same name but different erasure. An
	 * {@link ExecutableDecl} does not overload itself.
	 *
	 * @param other The other {@link ExecutableDecl} to check for overloading
	 * @throws NullPointerException if {@code other} is null
	 * @return whether the two {@link ExecutableDecl} override each other
	 */
	public boolean isOverloading(ExecutableDecl other) {
		return Objects.equals(getSimpleName(), Objects.requireNonNull(other).getSimpleName()) &&
			!hasSameErasure(other) &&
			containingType.isSameHierarchy(other.getContainingType());
	}

	/**
	 * Checks whether this executable accepts a variable number of arguments.
	 *
	 * @return true if this executable is varargs
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

	/**
	 * Returns the subset of this executable's thrown exceptions that are checked exceptions.
	 *
	 * @return the thrown checked exceptions
	 */
	public List<ITypeReference> getThrownCheckedExceptions() {
		return thrownExceptions.stream()
			.filter(e -> e.isSubtypeOf(TypeReference.EXCEPTION) && !e.isSubtypeOf(TypeReference.RUNTIME_EXCEPTION))
			.toList();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
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
