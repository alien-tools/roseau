package com.github.maracas.roseau.model;

import java.util.List;

/**
 * Represents a field declaration in a Java type.
 * This class extends the {@link Symbol} class and contains information about the field's data type and the {@link TypeDecl} to which it belongs.
 */
public final class FieldDecl extends Symbol {
	/**
	 * The data type of the field (e.g., int, double, class types, interface types).
	 */
	private final TypeReference type;

	public FieldDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, String position, TypeReference containingType, TypeReference type) {
		super(qualifiedName, visibility, modifiers, position, containingType);
		this.type = type;
	}

	/**
	 * Retrieves the data type of the field (e.g., int, double, class types, interface types).
	 *
	 * @return Field's data type
	 */
	public TypeReference getType() {
		return type;
	}

	/**
	 * Generates a string representation of the FieldDeclaration.
	 *
	 * @return A formatted string containing the field's name, data type, type, visibility,
	 * modifiers, and position.
	 */
	@Override
	public String toString() {
		return """
			Field %s [%s] [%s]
			  Position: %s
			""".formatted(qualifiedName, visibility, modifiers, position);
	}
}
