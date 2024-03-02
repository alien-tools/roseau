package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;

public final class EnumDecl extends ClassDecl {
	public EnumDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers,
	                List<Annotation> annotations, SourceLocation location,
	                List<TypeReference<InterfaceDecl>> implementedInterfaces, List<FieldDecl> fields,
	                List<MethodDecl> methods, TypeReference<TypeDecl> enclosingType, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, Collections.emptyList(),
			fields, methods, enclosingType, null, constructors);
	}

	@Override
	public boolean isEnum() {
		return true;
	}

	@Override
	public String toString() {
		return """
			%s enum %s
			  %s
			  %s
			""".formatted(visibility, qualifiedName, fields, methods);
	}
}
