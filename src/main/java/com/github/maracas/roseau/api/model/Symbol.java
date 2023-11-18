package com.github.maracas.roseau.api.model;

import java.util.List;

/**
 * This abstract class represents a symbol in the library, which can be a type,
 * a method, a constructor, or a field.
 * <p>
 * It provides information about the symbol's qualified name, visibility, modifiers,
 * and position within the source code.
 */
public abstract sealed class Symbol permits TypeDecl, TypeMemberDecl {
	/**
	 * The name of the symbol.
	 */
	protected final String qualifiedName;

	/**
	 * The visibility of the symbol.
	 */
	protected final AccessModifier visibility;

	/**
	 * Is the symbol accessible/exported?
	 */
	protected final boolean isExported;

	/**
	 * List of non-access modifiers applied to the symbol.
	 */
	protected final List<Modifier> modifiers;

	/**
	 * The exact location of the symbol
	 */
	protected final SourceLocation location;

	protected final TypeReference<TypeDecl> containingType;

	protected Symbol(String qualifiedName, AccessModifier visibility, boolean isExported, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType) {
		this.qualifiedName = qualifiedName;
		this.visibility = visibility;
		this.isExported = isExported;
		this.modifiers = modifiers;
		this.location = location;
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
	 * Checks whether the symbol is accessible/exported
	 *
	 * @return exported or not
	 */
	public boolean isExported() {
		return isExported;
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
	public SourceLocation getLocation() {
		return location;
	}

	public TypeReference<TypeDecl> getContainingType() {
		return containingType;
	}
}
