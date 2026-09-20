package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Set;

/**
 * A constructor declaration in a {@link ClassDecl}.
 */
public final class ConstructorDecl extends ExecutableDecl {
	public ConstructorDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                       Set<Annotation> annotations, SourceLocation location, TypeReference<TypeDecl> containingType,
	                       ITypeReference type, List<ParameterDecl> parameters,
	                       List<FormalTypeParameter> formalTypeParameters,
	                       Set<ITypeReference> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type,
			parameters, formalTypeParameters, thrownExceptions);
	}

	@Override
	public boolean isConstructor() {
		return true;
	}
}
