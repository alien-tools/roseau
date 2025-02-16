package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
			var erasedType = switch (p.type()) {
				case WildcardTypeReference wtr -> wtr.bounds().getFirst();
				case TypeParameterReference tpr -> resolveTypeParameterBound(tpr).orElse(TypeReference.OBJECT);
				default -> p.type();
			};
			sb.append(erasedType.getQualifiedName());
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
	 * Checks whether the given ExecutableDecl has the same erasure as the current instance.
	 *
	 * @param other The ExecutableDecl to compare the erasure with
	 * @return true if they have the same erasure, false otherwise
	 */
	public boolean hasSameErasure(ExecutableDecl other) {
		return Objects.equals(getErasure(), Objects.requireNonNull(other).getErasure());
	}

	public Optional<FormalTypeParameter> resolveTypeParameter(TypeParameterReference tpr) {
		var resolved = formalTypeParameters.stream()
			.filter(ftp -> ftp.name().equals(tpr.getQualifiedName()))
			.findFirst();

		return resolved.or(() -> containingType.getResolvedApiType().flatMap(t -> t.resolveTypeParameter(tpr)));
	}

	public Optional<ITypeReference> resolveTypeParameterBound(TypeParameterReference tpr) {
		var ftp = resolveTypeParameter(tpr);

		if (ftp.isPresent()) {
			if (ftp.get().bounds().getFirst() instanceof TypeParameterReference tpr2) {
				return resolveTypeParameterBound(tpr2);
			} else {
				return Optional.of(ftp.get().bounds().getFirst());
			}
		} else {
			return containingType.getResolvedApiType().flatMap(t -> t.resolveTypeParameterBound(tpr));
		}
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
