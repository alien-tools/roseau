package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Set;

/**
 * A field declaration in a {@link TypeDecl}.
 */
public final class FieldDecl extends TypeMemberDecl {
	public FieldDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                 List<Annotation> annotations, SourceLocation location, TypeReference<TypeDecl> containingType,
	                 ITypeReference type) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type);
	}

	/**
	 * Checks whether the current field shadows the supplied field.
	 *
	 * @param other The other field
	 * @return whether this shadows {@code other}
	 */
	public boolean isShadowing(FieldDecl other) {
		return getSimpleName().equals(other.getSimpleName()) &&
			getContainingType().isSubtypeOf(other.getContainingType());
	}

	@Override
	public String toString() {
		return "%s %s %s".formatted(visibility, type, qualifiedName);
	}

	@Override
	public FieldDecl deepCopy() {
		return new FieldDecl(qualifiedName, visibility, modifiers, annotations.stream().map(Annotation::deepCopy).toList(),
			location, containingType.deepCopy(), type.deepCopy());
	}
}
