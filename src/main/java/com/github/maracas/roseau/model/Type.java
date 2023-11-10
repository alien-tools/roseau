package com.github.maracas.roseau.model;

import java.util.ArrayList;
import java.util.Collection;
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
	 * The superclass as a type declaration (null if there isn't any).
	 */
	private Type superClass;

	private List<Type> superInterfaces;

	/**
	 * List of formal type parameters for generic types.
	 */
	private final List<String> formalTypeParameters;

	private final List<List<String>> formalTypeParamsBounds;

	/**
	 * A flag indicating whether the type is nested within another type.
	 */
	private final boolean isNested;

	private final boolean isCheckedException;

	public Type(String name, AccessModifier visibility, DeclarationKind kind, List<Modifier> modifiers,
	            List<String> referencedTypes, List<String> formalTypeParameters, List<List<String>> formalTypeParamsBounds,
	            boolean isNested, boolean isCheckedException, String position,
	            List<Field> fields, List<Method> methods, List<Constructor> constructors) {
		super(name, visibility, modifiers, referencedTypes, position);
		this.kind = kind;
		this.formalTypeParameters = formalTypeParameters;
		this.formalTypeParamsBounds = formalTypeParamsBounds;
		this.isNested = isNested;
		this.isCheckedException = isCheckedException;
		this.fields = fields;
		this.methods = methods;
		this.constructors = constructors;
	}

	public List<Method> getAllMethods() {
		List<Method> all = new ArrayList<>();

		all.addAll(methods);

		if (superClass != null)
			all.addAll(superClass.getAllMethods());

		all.addAll(superInterfaces.stream()
			.map(Type::getAllMethods)
			.flatMap(Collection::stream)
			.toList());

		return all;
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
	 * Retrieves all the superclasses of the type as typeDeclarations.
	 *
	 * @return Type's superclasses as typeDeclarations
	 */
	public Type getSuperclass() {
		return superClass;
	}

	/**
	 * Retrieves the superinterfaces of the type as typeDeclarations.
	 *
	 * @return Type's superinterfaces as typeDeclarations
	 */
	public List<Type> getSuperInterfaces() {
		return superInterfaces;
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
		return isNested;
	}

	public boolean isCheckedException() { return isCheckedException; }

	/**
	 * Sets the type's superclass.
	 *
	 * @param superClass The superclass to be set
	 */
	public void setSuperClass(Type superClass) {
		this.superClass = superClass;
	}

	/**
	 * Sets the type's superinterfaces.
	 *
	 * @param superInterfaces The superinterfaces to be set
	 */
	public void setSuperInterfaces(List<Type> superInterfaces) {
		this.superInterfaces = superInterfaces;
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


