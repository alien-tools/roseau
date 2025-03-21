package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.EnumSet;
import java.util.List;

public final class EnumValueDecl extends TypeMemberDecl {
	public EnumValueDecl(String qualifiedName, List<Annotation> annotations, SourceLocation location,
	                     TypeReference<TypeDecl> containingType, ITypeReference type) {
		super(qualifiedName, AccessModifier.PUBLIC, EnumSet.of(Modifier.FINAL, Modifier.STATIC),
			annotations, location, containingType, type);
	}

	@Override
	public String toString() {
		return simpleName;
	}

	@Override
	public EnumValueDecl deepCopy() {
		return new EnumValueDecl(qualifiedName, annotations.stream().map(Annotation::deepCopy).toList(),
			location, containingType.deepCopy(), type.deepCopy());
	}
}
