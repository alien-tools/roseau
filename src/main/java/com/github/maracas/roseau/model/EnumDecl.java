package com.github.maracas.roseau.model;

import java.util.Collections;
import java.util.List;

public final class EnumDecl extends ClassDecl {
	public EnumDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, String position, TypeReference containingType, List<TypeReference> superInterfaces, List<FieldDecl> fields, List<MethodDecl> methods, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, position, containingType, superInterfaces, Collections.emptyList(), fields, methods, null, constructors);
	}

	@Override
	public boolean isEnum() {
		return true;
	}

	@Override
	public String toString() {
		return """
			Enum %s [%s] [%s]
				Containing type: %s
			  Position: %s
			  Fields: %s
			  Methods: %s
			""".formatted(qualifiedName, visibility, modifiers, containingType, position, fields, methods);
	}
}
