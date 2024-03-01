package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;

public final class RecordDecl extends ClassDecl {
	public RecordDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, List<Annotation> annotations,
	                  SourceLocation location, List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                  List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                  TypeReference<TypeDecl> enclosingType, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, formalTypeParameters,
			fields, methods, enclosingType, null, constructors);
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
