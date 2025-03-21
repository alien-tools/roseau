package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A method declaration is a kind of {@link ExecutableDecl} within a {@link TypeDecl}.
 */
public final class MethodDecl extends ExecutableDecl {
	public MethodDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                  List<Annotation> annotations, SourceLocation location, TypeReference<TypeDecl> containingType,
	                  ITypeReference type, List<ParameterDecl> parameters, List<FormalTypeParameter> formalTypeParameters,
	                  List<ITypeReference> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type, parameters,
			formalTypeParameters, thrownExceptions);
	}

	@Override
	public boolean isMethod() {
		return true;
	}

	/**
	 * Returns whether the method has the modifier {@link Modifier#DEFAULT}.
	 *
	 * @return true if the method is default
	 */
	public boolean isDefault() {
		return modifiers.contains(Modifier.DEFAULT);
	}

	/**
	 * Returns whether the method has the modifier {@link Modifier#ABSTRACT}.
	 *
	 * @return true if the method is abstract
	 */
	public boolean isAbstract() {
		return modifiers.contains(Modifier.ABSTRACT);
	}

	/**
	 * Returns whether the method has the modifier {@link Modifier#NATIVE}.
	 *
	 * @return true if the method is native
	 */
	public boolean isNative() {
		return modifiers.contains(Modifier.NATIVE);
	}

	/**
	 * Returns whether the method has the modifier {@link Modifier#STRICTFP}.
	 *
	 * @return true if the method is strictfp
	 */
	public boolean isStrictFp() {
		return modifiers.contains(Modifier.STRICTFP);
	}

	/**
	 * Checks whether this method is effectively final. A method is effectively final if it has the modifier
	 * {@link Modifier#FINAL} or if it is declared in a {@link TypeDecl} that is itself effectively final.
	 *
	 * @return true if this method is effectively final
	 * @see TypeDecl#isEffectivelyFinal()
	 */
	public boolean isEffectivelyFinal() {
		return isFinal() || containingType.isEffectivelyFinal();
	}

	/**
	 * Checks whether this method overrides the supplied method {@code other}. A method overrides itself.
	 *
	 * @param other The other method
	 * @return whether this overrides {@code other}
	 * @throws NullPointerException if {@code other} is null
	 */
	public boolean isOverriding(MethodDecl other) {
		Objects.requireNonNull(other);
		if (equals(other)) {
			return true;
		}
		if (hasSameErasure(other)) {
			if (getContainingType().isSubtypeOf(other.getContainingType())) {
				return true;
			}
			if (!isAbstract() && other.isAbstract()) {
				return true;
			}
			if (!isDefault() && !isAbstract() && other.isDefault()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "%s %s %s %s(%s)".formatted(
			visibility,
			modifiers.stream().map(Modifier::toString).collect(Collectors.joining(" ")),
			type,
			getSimpleName(),
			parameters.stream().map(ParameterDecl::toString).collect(Collectors.joining(", ")));
	}

	@Override
	public MethodDecl deepCopy() {
		return new MethodDecl(qualifiedName, visibility, modifiers, annotations.stream().map(Annotation::deepCopy).toList(),
			location, containingType.deepCopy(), type.deepCopy(), parameters.stream().map(ParameterDecl::deepCopy).toList(),
			formalTypeParameters.stream().map(FormalTypeParameter::deepCopy).toList(),
			ITypeReference.deepCopy(thrownExceptions));
	}
}
