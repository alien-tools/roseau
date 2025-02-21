package com.github.maracas.roseau.api.model;

import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An abstract symbol (i.e., named entity) in the API: either a {@link TypeDecl} or a {@link TypeMemberDecl}.
 * Symbols have a fully qualified name, a visibility, a set of modifiers, a physical location, and may be annotated.
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
	protected final Set<Modifier> modifiers;

	/**
	 * Symbol annotations
	 */
	protected final List<Annotation> annotations;

	/**
	 * The exact physical location of the symbol
	 */
	protected final SourceLocation location;

	/**
	 * The simple (unqualified) name of the symbol
	 */
	protected final String simpleName;

	protected Symbol(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                 List<Annotation> annotations, SourceLocation location) {
		this.qualifiedName = Objects.requireNonNull(qualifiedName);
		this.visibility = Objects.requireNonNull(visibility);
		this.modifiers = Objects.requireNonNull(modifiers);
		this.annotations = Objects.requireNonNull(annotations);
		this.location = Objects.requireNonNull(location);
		this.simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
	}

	public String getQualifiedName() {
		return qualifiedName;
	}

	public AccessModifier getVisibility() {
		return visibility;
	}

	public Set<Modifier> getModifiers() {
		return Sets.immutableEnumSet(modifiers);
	}

	public List<Annotation> getAnnotations() {
		return Collections.unmodifiableList(annotations);
	}

	public SourceLocation getLocation() {
		return location;
	}

	public String getSimpleName() {
		return simpleName;
	}

	public boolean isPublic() {
		return AccessModifier.PUBLIC == visibility;
	}

	public boolean isProtected() {
		return AccessModifier.PROTECTED == visibility;
	}

	public boolean isPrivate() {
		return AccessModifier.PRIVATE == visibility;
	}

	public boolean isPackagePrivate() {
		return AccessModifier.PACKAGE_PRIVATE == visibility;
	}

	public boolean isStatic() {
		return modifiers.contains(Modifier.STATIC);
	}

	public boolean isFinal() {
		return modifiers.contains(Modifier.FINAL);
	}

	/**
	 * Checks whether the symbol is accessible/exported in the API
	 */
	public abstract boolean isExported();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Symbol symbol = (Symbol) o;
		return Objects.equals(qualifiedName, symbol.qualifiedName)
			&& Objects.equals(visibility, symbol.visibility)
			&& Objects.equals(modifiers, symbol.modifiers)
			&& Objects.equals(annotations, symbol.annotations)
			&& Objects.equals(location, symbol.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(qualifiedName, visibility, modifiers, annotations, location);
	}
}
