package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;

public final class EnumDecl extends ClassDecl {
	@JsonCreator
	public EnumDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType, List<TypeReference<InterfaceDecl>> superInterfaces, List<FieldDecl> fields, List<MethodDecl> methods, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, location, containingType, superInterfaces, Collections.emptyList(), fields, methods, null, constructors);
	}

	@Override
	public boolean isEnum() {
		return true;
	}

	@Override
	public String toString() {
		return """
			enum %s [%s]
			  %s
			  %s
			""".formatted(qualifiedName, visibility, fields, methods);
	}
}
