package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Set;

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

	public boolean isEquals() {
		return simpleName.equals("equals") &&
			type.equals(PrimitiveTypeReference.BOOLEAN) &&
			parameters.size() == 1 &&
			parameters.getFirst().type().equals(TypeReference.OBJECT);
	}

	public boolean isHashCode() {
		return simpleName.equals("hashCode") &&
			type.equals(PrimitiveTypeReference.INT) &&
			parameters.isEmpty();
	}

	public boolean isToString() {
		return simpleName.equals("toString") &&
			type.equals(TypeReference.STRING) &&
			parameters.isEmpty();
	}

	public boolean isValueOf() {
		return isStatic() &&
			simpleName.equals("valueOf") &&
			parameters.size() == 1 &&
			parameters.getFirst().type().equals(TypeReference.STRING);
	}

	public boolean isValues() {
		return isStatic() &&
			simpleName.equals("values") &&
			type.equals(new ArrayTypeReference(containingType, 1)) &&
			parameters.isEmpty();
	}
}
