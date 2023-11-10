package com.github.maracas.roseau.model;

import java.util.List;

/**
 * This abstract class represents an element declaration, which can be a type,
 * a method, a constructor, or a field in the library.
 * <p>
 * It provides information about the element's qualified name, visibility, non-access modifiers, referenced types,
 * and position within the source code.
 */
public abstract class Element {
	/**
	 * The name of the element.
	 */
	private final String name;

	/**
	 * The visibility of the element.
	 */
	private final AccessModifier visibility;

	/**
	 * List of non-access modifiers applied to the element.
	 */
	private final List<Modifier> modifiers;

	/**
	 * List of types referenced by the element.
	 */
	private final List<String> referencedTypes;

	/**
	 * The exact position of the element declaration
	 */
	private final String position;

	protected Element(String name, AccessModifier visibility, List<Modifier> modifiers, List<String> referencedTypes, String position) {
		this.name = name;
		this.visibility = visibility;
		this.modifiers = modifiers;
		this.referencedTypes = referencedTypes;
		this.position = position;
	}

	/**
	 * Retrieves the name of the element.
	 *
	 * @return Element's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the visibility of the element.
	 *
	 * @return Element's visibility
	 */
	public AccessModifier getVisibility() {
		return visibility;
	}

	/**
	 * Retrieves the list of non-access modifiers applied to the element.
	 *
	 * @return Element's non-access modifiers
	 */
	public List<Modifier> getModifiers() {
		return modifiers;
	}

	/**
	 * Retrieves the list of types referenced by this element.
	 *
	 * @return List of types referenced by the element
	 */
	public List<String> getReferencedTypes() {
		return referencedTypes;
	}

	/**
	 * Retrieves the position of the element declaration.
	 *
	 * @return Element's position.
	 */
	public String getPosition() {
		return position;
	}
}
