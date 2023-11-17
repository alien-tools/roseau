package com.github.maracas.roseau.api.model;

import java.util.List;

public final class RecordDecl extends ClassDecl {
	public RecordDecl(String qualifiedName, AccessModifier visibility, boolean isExported, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType, List<TypeReference<InterfaceDecl>> superInterfaces, List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, isExported, modifiers, location, containingType, superInterfaces, formalTypeParameters, fields, methods, null, constructors);
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
			""".formatted(qualifiedName, visibility, modifiers, containingType, location, fields, methods);
	}
}
