package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;

public final class InterfaceDecl extends TypeDecl {
	@JsonCreator
	public InterfaceDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers,
	                     SourceLocation location, List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                     List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields,
	                     List<MethodDecl> methods, TypeReference<TypeDecl> enclosingType) {
		super(qualifiedName, visibility, modifiers, location, implementedInterfaces, formalTypeParameters,
			fields, methods, enclosingType);
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
