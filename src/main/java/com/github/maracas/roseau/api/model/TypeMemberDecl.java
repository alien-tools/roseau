package com.github.maracas.roseau.api.model;

import java.util.List;

public abstract sealed class TypeMemberDecl extends Symbol implements TypeMember permits FieldDecl, ExecutableDecl {
	protected TypeMemberDecl(String qualifiedName, AccessModifier visibility, boolean isExported, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType) {
		super(qualifiedName, visibility, isExported, modifiers, location, containingType);
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
		return modifiers.contains(Modifier.FINAL) || modifiers.contains(Modifier.SEALED);
	}

	@Override
	public boolean isNative() {
		return modifiers.contains(Modifier.NATIVE);
	}

	@Override
	public boolean isStrictFp() {
		return modifiers.contains(Modifier.STRICTFP);
	}
}
