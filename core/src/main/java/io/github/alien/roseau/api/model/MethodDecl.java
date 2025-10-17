package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A method declaration is a kind of {@link ExecutableDecl} within a {@link TypeDecl}.
 */
public sealed class MethodDecl extends ExecutableDecl permits AnnotationMethodDecl {
	public MethodDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                  Set<Annotation> annotations, SourceLocation location, TypeReference<TypeDecl> containingType,
	                  ITypeReference type, List<ParameterDecl> parameters, List<FormalTypeParameter> formalTypeParameters,
	                  Set<ITypeReference> thrownExceptions) {
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

	@Override
	public String toString() {
		return "%s %s%s%s %s(%s)".formatted(
			visibility,
			modifiers.isEmpty()
				? ""
				: modifiers.stream().map(Modifier::toString).collect(Collectors.joining(" ")) + " ",
			formalTypeParameters.isEmpty()
				? ""
				: "<" + formalTypeParameters.stream().map(FormalTypeParameter::toString).collect(Collectors.joining(", ")) + "> ",
			type,
			getSimpleName(),
			parameters.stream().map(ParameterDecl::toString).collect(Collectors.joining(", ")));
	}
}
