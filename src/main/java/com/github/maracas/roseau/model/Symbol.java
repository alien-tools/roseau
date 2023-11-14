package com.github.maracas.roseau.model;

import java.util.List;

/**
 * This abstract class represents a symbol in the library, which can be a type,
 * a method, a constructor, or a field.
 * <p>
 * It provides information about the symbol's qualified name, visibility, modifiers,
 * and position within the source code.
 */
public abstract sealed class Symbol permits TypeDecl, ExecutableDecl, FieldDecl {
	/**
	 * The name of the symbol.
	 */
	protected final String qualifiedName;

	/**
	 * The visibility of the symbol.
	 */
	protected final AccessModifier visibility;

	/**
	 * List of non-access modifiers applied to the symbol.
	 */
	protected final List<Modifier> modifiers;

	/**
	 * The exact position of the symbol
	 */
	protected final String position;

	protected final TypeReference containingType;

	protected Symbol(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, String position, TypeReference containingType) {
		this.qualifiedName = qualifiedName;
		this.visibility = visibility;
		this.modifiers = modifiers;
		this.position = position;
		this.containingType = containingType;
	}

	/**
	 * Retrieves the name of the symbol.
	 *
	 * @return The symbol's name
	 */
	public String getQualifiedName() {
		return qualifiedName;
	}

	/**
	 * Retrieves the visibility of the symbol.
	 *
	 * @return The symbol's visibility
	 */
	public AccessModifier getVisibility() {
		return visibility;
	}

	/**
	 * Retrieves the list of non-access modifiers applied to the symbol.
	 *
	 * @return The symbol's non-access modifiers
	 */
	public List<Modifier> getModifiers() {
		return modifiers;
	}

	/**
	 * Retrieves the position of the symbol.
	 *
	 * @return The symbol's position.
	 */
	public String getPosition() {
		return position;
	}

	public TypeReference getContainingType() {
		return containingType;
	}
}
