package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.EnumSet;
import java.util.Set;

/**
 * An {@link EnumDecl} value.
 */
public final class EnumValueDecl extends TypeMemberDecl {
	public EnumValueDecl(String qualifiedName, Set<Annotation> annotations, SourceLocation location,
	                     TypeReference<TypeDecl> containingType, ITypeReference type) {
		super(qualifiedName, AccessModifier.PUBLIC, EnumSet.of(Modifier.FINAL, Modifier.STATIC),
			annotations, location, containingType, type);
	}
}
