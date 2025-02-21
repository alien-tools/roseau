package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConstructorDecl extends ExecutableDecl {
	public ConstructorDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                       List<Annotation> annotations, SourceLocation location, TypeReference<TypeDecl> containingType,
	                       ITypeReference type, List<ParameterDecl> parameters,
	                       List<FormalTypeParameter> formalTypeParameters,
	                       List<ITypeReference> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type,
			parameters, formalTypeParameters, thrownExceptions);
	}

	@Override
	public boolean isConstructor() {
		return true;
	}

	@Override
	public String toString() {
		return "%s %s(%s)".formatted(visibility, getSimpleName(),
			parameters.stream().map(ParameterDecl::toString).collect(Collectors.joining(", ")));
	}
}
