package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Set;

/**
 * A field declaration in a {@link TypeDecl}.
 */
public final class FieldDecl extends TypeMemberDecl {
	private final boolean compileTimeConstant;

	public FieldDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                 Set<Annotation> annotations, SourceLocation location, TypeReference<TypeDecl> containingType,
	                 ITypeReference type, boolean compileTimeConstant) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type);
		this.compileTimeConstant = compileTimeConstant;
	}

	public boolean isCompileTimeConstant() {
		return compileTimeConstant;
	}

	@Override
	public String toString() {
		return "%s %s %s".formatted(visibility, type, simpleName);
	}
}
