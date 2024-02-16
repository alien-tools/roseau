package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;

public abstract sealed class TypeMemberDecl extends Symbol implements TypeMember permits FieldDecl, ExecutableDecl {
	protected final TypeReference<TypeDecl> containingType;
	protected final ITypeReference type;

	protected TypeMemberDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, List<Annotation> annotations,
	                         SourceLocation location, TypeReference<TypeDecl> containingType, ITypeReference type) {
		super(qualifiedName, visibility, modifiers, annotations, location);
		this.containingType = containingType;
		this.type = type;
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
		return (isPublic()
			|| (isProtected() && !containingType.getResolvedApiType().map(TypeDecl::isEffectivelyFinal).orElse(true)))
			&& containingType.getResolvedApiType().map(TypeDecl::isExported).orElse(true);
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
		return Objects.equals(type, other.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), type);
	}
}
