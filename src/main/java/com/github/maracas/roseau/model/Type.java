package com.github.maracas.roseau.model;

import java.util.List;

/**
 * Represents a type declaration in the library, such as a class, an interface, or an enum.
 * This class extends the {@link Element} class and contains information about the type's kind, fields, methods, constructors, and more.
 */
public class Type extends Element {
	/**
	 * The type of the declaration (e.g., class, interface, enum).
	 */
	private final DeclarationKind kind;

	/**
	 * List of fields declared within the type.
	 */
	private final List<Field> fields;

	/**
	 * List of methods declared within the type.
	 */
	private final List<Method> methods;

	/**
	 * List of constructors declared within the type.
	 */
	private final List<Constructor> constructors;

	/**
	 * The qualified name of the superclass ("None" if there isn't any).
	 */
	private final String superclassName;

	/**
	 * The superclass as a type declaration (null if there isn't any).
	 */
	private List<Type> allSuperclasses;

	private List<Type> superinterfaces;

	/**
	 * The qualified names of the interfaces implemented by the type.
	 */
	private final List<String> superinterfacesNames;

	/**
	 * List of formal type parameters for generic types.
	 */
	private final List<String> formalTypeParameters;

	private final List<List<String>> formalTypeParamsBounds;

	/**
	 * A flag indicating whether the type is nested within another type.
	 */
	private final boolean nested;

	public Type(String name, AccessModifier visibility, DeclarationKind kind, List<Modifier> modifiers,
	            String superclassName, List<String> superinterfacesNames, List<String> referencedTypes,
	            List<String> formalTypeParameters, List<List<String>> formalTypeParamsBounds, boolean nested, String position,
	            List<Field> fields, List<Method> methods, List<Constructor> constructors) {
		super(name, visibility, modifiers, referencedTypes, position);
		this.kind = kind;
		this.superclassName = superclassName;
		this.superinterfacesNames = superinterfacesNames;
		this.formalTypeParameters = formalTypeParameters;
		this.formalTypeParamsBounds = formalTypeParamsBounds;
		this.nested = nested;
		this.fields = fields;
		this.methods = methods;
		this.constructors = constructors;
	}

	/**
	 * Retrieves the type of the declaration (e.g., class, interface, enum).
	 *
	 * @return Type's kind
	 */
	public DeclarationKind getDeclarationType() {
		return kind;
	}

	/**
	 * Retrieves the list of fields declared within the type.
	 *
	 * @return List of fields declared within the type
	 */
	public List<Field> getFields() {
		return fields;
	}

	/**
	 * Retrieves the list of methods declared within the type.
	 *
	 * @return List of methods declared within the type
	 */
	public List<Method> getMethods() {
		return methods;
	}

	/**
	 * Retrieves the list of constructors declared within the type.
	 *
	 * @return List of constructors declared within the type
	 */
	public List<Constructor> getConstructors() {
		return constructors;
	}

	/**
	 * Retrieves the qualified name of the type's superclass ("None" if there isn't any).
	 *
	 * @return Superclass's qualified name
	 */
	public String getSuperclassName() {
		return superclassName;
	}

	/**
	 * Retrieves the qualified names of the interfaces implemented by the type.
	 *
	 * @return Qualified names of the interfaces implemented by the type
	 */
	public List<String> getSuperinterfacesNames() {
		return superinterfacesNames;
	}

	/**
	 * Retrieves all the superclasses of the type as typeDeclarations.
	 *
	 * @return Type's superclasses as typeDeclarations
	 */
	public List<Type> getAllSuperclasses() {
		return allSuperclasses;
	}

	/**
	 * Retrieves the superinterfaces of the type as typeDeclarations.
	 *
	 * @return Type's superinterfaces as typeDeclarations
	 */
	public List<Type> getSuperinterfaces() {
		return superinterfaces;
	}

	/**
	 * Retrieves the list of formal type parameters for generic types.
	 *
	 * @return List of formal type parameters
	 */
	public List<String> getFormalTypeParameters() {
		return formalTypeParameters;
	}

	/**
	 * Retrieves a list of lists containing the formal type parameters' bounds.
	 *
	 * @return formal type parameters bounds
	 */
	public List<List<String>> getFormalTypeParamsBounds() {
		return formalTypeParamsBounds;
	}

	/**
	 * Checks if the type is nested within another type.
	 *
	 * @return True if the type is nested; false otherwise
	 */
	public boolean isNested() {
		return nested;
	}

	/**
	 * Sets the type's superclasses.
	 *
	 * @param allSuperclasses The superclasses to be set
	 */
	public void setAllSuperclasses(List<Type> allSuperclasses) {
		this.allSuperclasses = allSuperclasses;
	}

	/**
	 * Sets the type's superinterfaces.
	 *
	 * @param superinterfaces The superinterfaces to be set
	 */
	public void setSuperinterfaces(List<Type> superinterfaces) {
		this.superinterfaces = superinterfaces;
	}

	/**
	 * Generates a string representation of the TypeDeclaration.
	 *
	 * @return A formatted string containing the type name, visibility, type kind,
	 * nesting status, position, fields, methods and constructors.
	 */
	@Override
	public String toString() {
		return "Type Name: " + getName() + "\n" +
			"Visibility: " + getVisibility() + "\n" +
			"Kind: " + getDeclarationType() + "\n" +
			"Modifiers: " + getModifiers() + "\n" +
			"Is Nested: " + isNested() + "\n" +
			"Position: " + getPosition() + "\n\n" +
			"Type's fields: " + getFields().toString() + "\n\n" +
			"Type's Methods: " + getMethods().toString() + "\n\n" +
			"Type's Constructors: " + getConstructors().toString() + "\n";

	}
}


