package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * A member of a type declaration, either a {@link FieldDecl} or {@link ExecutableDecl}.
 * Type members have a type and belong to some containing type.
 */
public abstract sealed class TypeMemberDecl extends Symbol implements TypeMember permits FieldDecl, ExecutableDecl {
	protected final TypeReference<TypeDecl> containingType;
	protected final ITypeReference type;

	protected TypeMemberDecl(String qualifiedName, AccessModifier visibility, EnumSet<Modifier> modifiers,
	                         List<Annotation> annotations, SourceLocation location,
	                         TypeReference<TypeDecl> containingType, ITypeReference type) {
		super(qualifiedName, visibility, modifiers, annotations, location);
		this.containingType = Objects.requireNonNull(containingType);
		this.type = Objects.requireNonNull(type);
	}

	@Override
	public TypeReference<TypeDecl> getContainingType() {
		return containingType;
	}

	@Override
	public ITypeReference getType() {
		return type;
	}

	@Override
	public boolean isExported() {
		return containingType.isExported() && (isPublic() || (isProtected() && !containingType.isEffectivelyFinal()));
	}

	@Override
	public boolean isStatic() {
		return modifiers.contains(Modifier.STATIC);
	}

	@Override
	public boolean isFinal() {
		return modifiers.contains(Modifier.FINAL);
	}

	@Override
	public boolean isPublic() {
		return AccessModifier.PUBLIC == visibility;
	}

	@Override
	public boolean isProtected() {
		return AccessModifier.PROTECTED == visibility;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		TypeMemberDecl other = (TypeMemberDecl) o;
		return Objects.equals(type, other.type) && Objects.equals(containingType, other.containingType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), type, containingType);
	}
}
