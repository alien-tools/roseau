package com.github.maracas.roseau.model;

import java.util.List;

/**
 * Represents a field declaration in a Java type.
 * This class extends the {@link ElementDeclaration} class and contains information about the field's data type and the {@link TypeDeclaration} to which it belongs.
 */
public class FieldDeclaration extends ElementDeclaration {
	/**
	 * The type containing the field.
	 */
	private TypeDeclaration type;

	/**
	 * The data type of the field (e.g., int, double, class types, interface types).
	 */
	private String dataType;

	public FieldDeclaration(String name, TypeDeclaration type, AccessModifier visibility, String dataType, List<NonAccessModifiers> Modifiers, List<String> referencedTypes, String position) {
		super(name, visibility, Modifiers, referencedTypes, position);
		this.type = type;
		this.dataType = dataType;
	}

	/**
	 * Retrieves the TypeDeclaration containing the field.
	 *
	 * @return Type containing the field
	 */
	public TypeDeclaration getType() {
		return type;
	}

	/**
	 * Retrieves the data type of the field (e.g., int, double, class types, interface types).
	 *
	 * @return Field's data type
	 */
	public String getDataType() {
		return dataType;
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
			"Data Type: " + getDataType() + "\n" +
			"Type: " + getType().getName() + "\n" +
			"Visibility: " + getVisibility() + "\n" +
			"Modifiers: " + getModifiers() + "\n" +
			"Position: " + getPosition() + "\n\n";
	}
}
