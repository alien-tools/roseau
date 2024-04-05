package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A method declaration within a {@link TypeDecl}.
 * Extends the {@link ExecutableDecl} class and complements it with method-specific information
 */
public final class MethodDecl extends ExecutableDecl {
	public MethodDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers,
	                  List<Annotation> annotations, SourceLocation location, TypeReference<TypeDecl> containingType,
	                  ITypeReference type, List<ParameterDecl> parameters, List<FormalTypeParameter> formalTypeParameters,
	                  List<TypeReference<ClassDecl>> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type, parameters,
			formalTypeParameters, thrownExceptions);
	}

	@Override
	public boolean isMethod() {
		return true;
	}

	public boolean isDefault() {
		return modifiers.contains(Modifier.DEFAULT);
	}

	public boolean isAbstract() {
		return modifiers.contains(Modifier.ABSTRACT);
	}

	public boolean isNative() {
		return modifiers.contains(Modifier.NATIVE);
	}

	public boolean isStrictFp() {
		return modifiers.contains(Modifier.STRICTFP);
	}

	public boolean isEffectivelyFinal() {
		return isFinal() || containingType.isEffectivelyFinal();
	}

	/**
	 * Checks whether the current method overrides the supplied method.
	 * A method overrides itself.
	 *
	 * @param other The other method
	 * @return whether this overrides other
	 */
	public boolean isOverriding(MethodDecl other) {
		Objects.requireNonNull(other);
		if (equals(other))
			return true;
		if (hasSameSignature(other)) {
			if (getContainingType().isSubtypeOf(other.getContainingType()))
				return true;
			if (!isAbstract() && other.isAbstract())
				return true;
			if (!isDefault() && !isAbstract() && other.isDefault())
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "%s %s %s %s(%s)".formatted(
			visibility,
			modifiers.stream().map(Object::toString).collect(Collectors.joining(" ")),
			type,
			getSimpleName(),
			parameters.stream().map(Object::toString).collect(Collectors.joining(", ")));
	}
}
