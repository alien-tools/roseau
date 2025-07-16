package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A member declared by a {@link TypeDecl}, either a {@link FieldDecl} or {@link ExecutableDecl}. Type members have a
 * type and belong to some containing type.
 */
public abstract sealed class TypeMemberDecl extends Symbol
	permits FieldDecl, ExecutableDecl, EnumValueDecl, RecordComponentDecl {
	protected final TypeReference<TypeDecl> containingType;
	protected final ITypeReference type;

	protected TypeMemberDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                         List<Annotation> annotations, SourceLocation location,
	                         TypeReference<TypeDecl> containingType, ITypeReference type) {
		super(qualifiedName, visibility, modifiers, annotations, location);
		Preconditions.checkNotNull(containingType);
		Preconditions.checkNotNull(type);
		Preconditions.checkArgument(Set.of(AccessModifier.PUBLIC, AccessModifier.PROTECTED).contains(visibility),
			"Type member declarations are either PUBLIC or PROTECTED");
		this.containingType = containingType;
		this.type = type;
	}

	public TypeReference<TypeDecl> getContainingType() {
		return containingType;
	}

	public ITypeReference getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		TypeMemberDecl other = (TypeMemberDecl) o;
		return Objects.equals(type, other.type) && Objects.equals(containingType, other.containingType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), type, containingType);
	}
}
