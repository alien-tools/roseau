package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;

public final class InterfaceDecl extends TypeDecl {
	@JsonCreator
	public InterfaceDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers,
	                     SourceLocation location, TypeReference<TypeDecl> containingType,
	                     List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                     List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields,
	                     List<MethodDecl> methods) {
		super(qualifiedName, visibility, modifiers, location, containingType, implementedInterfaces, formalTypeParameters,
			fields, methods);
	}

	@Override
	public boolean isInterface() {
		return true;
	}

	@Override
	public String toString() {
		return """
			interface %s [%s]
			  %s
			  %s
			""".formatted(qualifiedName, visibility, fields, methods);
	}
}
