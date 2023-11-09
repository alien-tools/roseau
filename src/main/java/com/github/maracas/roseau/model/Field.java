package com.github.maracas.roseau.model;

import java.util.List;

/**
 * Represents a field declaration in a Java type.
 * This class extends the {@link Element} class and contains information about the field's data type and the {@link Type} to which it belongs.
 */
public class Field extends Element {
	/**
	 * The type containing the field.
	 */
	private final Type declaringType;

	/**
	 * The data type of the field (e.g., int, double, class types, interface types).
	 */
	private final String type;

	public Field(String name, Type declaringType, AccessModifier visibility, String type, List<NonAccessModifiers> modifiers, List<String> referencedTypes, String position) {
		super(name, visibility, modifiers, referencedTypes, position);
		this.declaringType = declaringType;
		this.type = type;
	}

	/**
	 * Retrieves the TypeDeclaration containing the field.
	 *
	 * @return Type containing the field
	 */
	public Type getDeclaringType() {
		return declaringType;
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
			"Declaring Type: " + getDeclaringType().getName() + "\n" +
			"Visibility: " + getVisibility() + "\n" +
			"Modifiers: " + getModifiers() + "\n" +
			"Position: " + getPosition() + "\n\n";
	}
}
