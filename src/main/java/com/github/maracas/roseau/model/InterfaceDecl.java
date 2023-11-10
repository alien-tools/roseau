package com.github.maracas.roseau.model;

import java.util.List;

public final class InterfaceDecl extends TypeDecl {
	public InterfaceDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, String position, TypeReference containingType, List<TypeReference> superInterfaces, List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods) {
		super(qualifiedName, visibility, modifiers, position, containingType, superInterfaces, formalTypeParameters, fields, methods);
	}

	@Override
	public boolean isInterface() {
		return true;
	}

	@Override
	public String toString() {
		return """
			Interface %s [%s] [%s]
				Containing type: %s
			  Position: %s
			  Fields: %s
			  Methods: %s
			""".formatted(qualifiedName, visibility, modifiers, containingType, position, fields, methods);
	}
}
