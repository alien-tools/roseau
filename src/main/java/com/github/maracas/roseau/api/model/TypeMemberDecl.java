package com.github.maracas.roseau.api.model;

import java.util.List;

public abstract sealed class TypeMemberDecl extends Symbol permits FieldDecl, ExecutableDecl {
	protected TypeMemberDecl(String qualifiedName, AccessModifier visibility, boolean isExported, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType) {
		super(qualifiedName, visibility, isExported, modifiers, location, containingType);
	}
}
