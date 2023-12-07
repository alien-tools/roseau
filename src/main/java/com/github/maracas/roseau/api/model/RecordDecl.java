package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;

public final class RecordDecl extends ClassDecl {
	@JsonCreator
	public RecordDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType, List<TypeReference<InterfaceDecl>> superInterfaces, List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, location, containingType, superInterfaces, formalTypeParameters, fields, methods, null, constructors);
	}

	@Override
	public boolean isRecord() {
		return true;
	}

	@Override
	public String toString() {
		return """
			record %s [%s]
			  %s
			  %s
			""".formatted(qualifiedName, visibility, fields, methods);
	}
}
