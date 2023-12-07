package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;

public abstract sealed class TypeMemberDecl extends Symbol implements TypeMember permits FieldDecl, ExecutableDecl {
	protected final ITypeReference type;

	protected TypeMemberDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType, ITypeReference type) {
		super(qualifiedName, visibility, modifiers, location, containingType);
		this.type = type;
	}

	@Override
	public ITypeReference getType() {
		return type;
	}

	@Override
	public boolean isExported() {
		return (isPublic() || (isProtected() && !containingType.getResolvedApiType().map(TypeDecl::isEffectivelyFinal).orElse(true)))
			&& containingType.getResolvedApiType().map(TypeDecl::isExported).orElse(true);
	}

	@Override
	public boolean isStatic() {
		return modifiers.contains(Modifier.STATIC);
	}

	@Override
	public boolean isPublic() {
		return visibility == AccessModifier.PUBLIC;
	}

	@Override
	public boolean isProtected() {
		return visibility == AccessModifier.PROTECTED;
	}

	@Override
	public boolean isFinal() {
		return modifiers.contains(Modifier.FINAL);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		TypeMemberDecl that = (TypeMemberDecl) o;
		return Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), type);
	}
}
