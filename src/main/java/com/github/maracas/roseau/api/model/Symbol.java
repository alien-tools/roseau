package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;

/**
 * This abstract class represents a symbol in the library, which can be a type,
 * a method, a constructor, or a field.
 * <p>
 * It provides information about the symbol's qualified qualifiedName, visibility, modifiers,
 * and position within the source code.
 */
public abstract sealed class Symbol permits TypeDecl, TypeMemberDecl {
	/**
	 * The qualifiedName of the symbol.
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
	 * The exact location of the symbol
	 */
	protected final SourceLocation location;

	protected final TypeReference<TypeDecl> containingType;

	protected Symbol(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, SourceLocation location,
	                 TypeReference<TypeDecl> containingType) {
		this.qualifiedName = qualifiedName;
		this.visibility = visibility;
		this.modifiers = modifiers;
		this.location = location;
		this.containingType = containingType;
	}

	/**
	 * Retrieves the qualifiedName of the symbol.
	 *
	 * @return The symbol's qualifiedName
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
	@JsonIgnore
	public abstract boolean isExported();

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Symbol symbol = (Symbol) o;
		return Objects.equals(qualifiedName, symbol.qualifiedName)
			&& visibility == symbol.visibility
			&& Objects.equals(modifiers, symbol.modifiers)
			&& Objects.equals(location, symbol.location)
			&& Objects.equals(containingType, symbol.containingType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(qualifiedName, visibility, modifiers, location, containingType);
	}
}
