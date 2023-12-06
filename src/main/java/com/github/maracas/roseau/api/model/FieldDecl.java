package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

/**
 * Represents a field declaration in a Java type.
 * This class extends the {@link Symbol} class and contains information about the field's data type and the {@link TypeDecl} to which it belongs.
 */
public final class FieldDecl extends TypeMemberDecl {
	@JsonCreator
	public FieldDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType, TypeReference<TypeDecl> type) {
		super(qualifiedName, visibility, modifiers, location, containingType, type);
	}

	@Override
	public String getSimpleName() {
		return qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
	}

	/**
	 * Generates a string representation of the FieldDeclaration.
	 *
	 * @return A formatted string containing the field's name, data type, type, visibility,
	 * modifiers, and position.
	 */
	@Override
	public String toString() {
		return "field %s [%s] [%s]".formatted(qualifiedName, visibility, type);
	}
}
