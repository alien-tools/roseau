package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;

public final class AnnotationDecl extends TypeDecl {
	@JsonCreator
	public AnnotationDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType, List<FieldDecl> fields, List<MethodDecl> methods) {
		super(qualifiedName, visibility, modifiers, location, containingType, Collections.emptyList(), Collections.emptyList(), fields, methods);
	}

	@Override
	public boolean isAnnotation() {
		return true;
	}

	@Override
	public String toString() {
		return """
			annotation %s [%s]
			  %s
			  %s
			""".formatted(qualifiedName, visibility, fields, methods);
	}
}
