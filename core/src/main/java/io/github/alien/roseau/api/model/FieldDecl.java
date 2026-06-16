package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Objects;
import java.util.Set;

/**
 * A field declaration in a {@link TypeDecl}.
 */
public final class FieldDecl extends TypeMemberDecl {
	private final boolean compileTimeConstant;

	public FieldDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                 Set<Annotation> annotations, SourceLocation location, TypeReference<TypeDecl> containingType,
	                 ITypeReference type, boolean compileTimeConstant) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type);
		this.compileTimeConstant = compileTimeConstant;
	}

	public boolean isCompileTimeConstant() {
		return compileTimeConstant;
	}

	@Override
	public String toString() {
		return "%s %s %s".formatted(visibility, type, simpleName);
	}


	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		return obj instanceof FieldDecl other
			&& compileTimeConstant == other.compileTimeConstant;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), compileTimeConstant);
	}
}
