package com.github.maracas.roseau.model;

import java.util.List;

/**
 * Represents a field declaration in a Java type.
 * This class extends the {@link Element} class and contains information about the field's data type and the {@link Type} to which it belongs.
 */
public class Field extends Element {
	/**
	 * The data type of the field (e.g., int, double, class types, interface types).
	 */
	private final String type;

	public Field(String name, AccessModifier visibility, String type, List<Modifier> modifiers, List<String> referencedTypes, String position) {
		super(name, visibility, modifiers, referencedTypes, position);
		this.type = type;
	}

	/**
	 * Retrieves the data type of the field (e.g., int, double, class types, interface types).
	 *
	 * @return Field's data type
	 */
	public String getType() {
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
		return "Field Name: " + getName() + "\n" +
			"Type: " + getType() + "\n" +
			"Visibility: " + getVisibility() + "\n" +
			"Modifiers: " + getModifiers() + "\n" +
			"Position: " + getPosition() + "\n\n";
	}
}
