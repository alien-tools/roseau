package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;

/**
 * Represents a field declaration in a Java type. This class extends the {@link Symbol} class and contains information
 * about the field's data type and the {@link TypeDecl} to which it belongs.
 */
public final class FieldDecl extends TypeMemberDecl {
	public FieldDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, List<Annotation> annotations,
	                 SourceLocation location, TypeReference<TypeDecl> containingType, ITypeReference type) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type);
	}

	@Override
	public String getSimpleName() {
		return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
	}

	/**
	 * Generates a string representation of the FieldDeclaration.
	 *
	 * @return A formatted string containing the field's qualifiedName, data type, type, visibility,
	 * modifiers, and position.
	 */
	@Override
	public String toString() {
		return "field %s [%s] [%s]".formatted(qualifiedName, visibility, type);
	}
}
