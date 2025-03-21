package io.github.alien.roseau.api.model;

import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An abstract symbol (i.e., named entity) in the API: either a {@link TypeDecl} or a {@link TypeMemberDecl}. Symbols
 * are part of an {@link API}, can be referenced in client code, and are subject to breaking changes. Symbols have a
 * fully qualified name, a visibility, a set of modifiers, a physical location, and may be annotated. Symbols are
 * immutable, except for lazily-resolved type references.
 */
public abstract sealed class Symbol implements DeepCopyable<Symbol> permits TypeDecl, TypeMemberDecl {
	/**
	 * Fully qualified name of the symbol, unique within an {@link API}'s scope. Types and fields are uniquely identified
	 * by their fully qualified name (e.g., {@code pkg.sub.T}). Methods are uniquely identified by the erasure of their
	 * fully qualified signature (e.g., {@code pkg.sub.T.m(int)})
	 */
	protected final String qualifiedName;

	/**
	 * The symbol's visibility (e.g., PUBLIC, PRIVATE)
	 */
	protected final AccessModifier visibility;

	/**
	 * The symbol's non-access modifiers (e.g., STATIC, FINAL)
	 */
	protected final Set<Modifier> modifiers;

	/**
	 * Annotations placed on the symbol (e.g., @Deprecated, @Beta)
	 */
	protected final List<Annotation> annotations;

	/**
	 * The exact physical location of the symbol in the source (e.g., /src/pkg/T.java:4)
	 */
	protected final SourceLocation location;

	/**
	 * The symbol's simple (unqualified) name
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

	/**
	 * Checks whether the symbol is exported/accessible from outside the API.
	 *
	 * @return true if this symbol can be accessed from outside the API
	 */
	public abstract boolean isExported();

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
		return visibility == AccessModifier.PUBLIC;
	}

	public boolean isProtected() {
		return visibility == AccessModifier.PROTECTED;
	}

	public boolean isPrivate() {
		return visibility == AccessModifier.PRIVATE;
	}

	public boolean isPackagePrivate() {
		return visibility == AccessModifier.PACKAGE_PRIVATE;
	}

	public boolean isStatic() {
		return modifiers.contains(Modifier.STATIC);
	}

	public boolean isFinal() {
		return modifiers.contains(Modifier.FINAL);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
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
