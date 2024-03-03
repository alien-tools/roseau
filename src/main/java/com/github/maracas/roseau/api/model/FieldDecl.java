package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;

/**
 * A field declaration in a {@link TypeDecl}.
 */
public final class FieldDecl extends TypeMemberDecl {
	public FieldDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers,
	                 List<Annotation> annotations, SourceLocation location, TypeReference<TypeDecl> containingType,
	                 ITypeReference type) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type);
	}

	/**
	 * Generates a string representation of the FieldDeclaration.
	 *
	 * @return A formatted string containing the field's qualifiedName, data type, type, visibility,
	 * modifiers, and position.
	 */
	@Override
	public String toString() {
		return "%s %s %s".formatted(visibility, type, qualifiedName);
	}
}
