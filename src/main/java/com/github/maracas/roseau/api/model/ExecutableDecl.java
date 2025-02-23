package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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

	public boolean isMethod() {
		return false;
	}

	public boolean isConstructor() {
		return false;
	}

	// §8.4.2
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
	 * Checks whether the given ExecutableDecl has the same erasure as the current instance.
	 *
	 * @param other The ExecutableDecl to compare the erasure with
	 * @return true if they have the same erasure, false otherwise
	 */
	public boolean hasSameErasure(ExecutableDecl other) {
		return Objects.equals(getErasure(), Objects.requireNonNull(other).getErasure());
	}

	public Optional<FormalTypeParameter> resolveTypeParameter(TypeParameterReference tpr) {
		return getFormalTypeParametersInScope().stream()
			.filter(ftp -> ftp.name().equals(tpr.getQualifiedName()))
			.findFirst();
	}

	public ITypeReference resolveTypeParameterBound(TypeParameterReference tpr) {
		var resolved = resolveTypeParameter(tpr);

		if (resolved.isPresent()) {
			var bound = resolved.get().bounds().getFirst();
			if (bound instanceof TypeParameterReference tpr2) {
				return resolveTypeParameterBound(tpr2);
			} else {
				return bound;
			}
		} else {
			return TypeReference.OBJECT;
		}
	}

	/**
	 * Returns the list of formal type parameters in this method's scope, including from its containing type
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
	 * Checks whether the supplied executable and this instance are overloading each others.
	 * We assume that input source code compiles, so we won't have two methods
	 * with same name and parameters but different return types.
	 * An executable does not overload itself.
	 *
	 * @param other The other executable
	 * @return whether the two executables override each other
	 */
	public boolean isOverloading(ExecutableDecl other) {
		return Objects.equals(getSimpleName(), other.getSimpleName()) &&
			!hasSameErasure(other) &&
			containingType.isSameHierarchy(other.getContainingType());
	}

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
