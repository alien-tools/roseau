package com.github.maracas.roseau.model;

import java.util.List;

public final class RecordDecl extends ClassDecl {
	public RecordDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, String position, TypeReference containingType, List<TypeReference> superInterfaces, List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, position, containingType, superInterfaces, formalTypeParameters, fields, methods, TypeReference.NULL, constructors);
	}

	@Override
	public boolean isRecord() {
		return true;
	}

	@Override
	public String toString() {
		return """
			Record %s [%s] [%s]
				Containing type: %s
			  Position: %s
			  Fields: %s
			  Methods: %s
			""".formatted(qualifiedName, visibility, modifiers, containingType, position, fields, methods);
	}
}
