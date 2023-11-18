package com.github.maracas.roseau.api.model;

import java.util.List;

public final class InterfaceDecl extends TypeDecl {
	public InterfaceDecl(String qualifiedName, AccessModifier visibility, boolean isExported, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType, List<TypeReference<InterfaceDecl>> superInterfaces, List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods) {
		super(qualifiedName, visibility, isExported, modifiers, location, containingType, superInterfaces, formalTypeParameters, fields, methods);
	}

	@Override
	public boolean isInterface() {
		return true;
	}

	@Override
	public String toString() {
		return """
			interface %s [%s] (%s)
			  %s
			  %s
			""".formatted(qualifiedName, visibility, location, fields, methods);
	}
}
