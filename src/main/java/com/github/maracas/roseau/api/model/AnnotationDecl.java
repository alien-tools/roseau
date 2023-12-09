package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;

public final class AnnotationDecl extends TypeDecl {
	@JsonCreator
	public AnnotationDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers,
	                      SourceLocation location, List<FieldDecl> fields, List<MethodDecl> methods,
	                      TypeReference<TypeDecl> enclosingType) {
		super(qualifiedName, visibility, modifiers, location, Collections.emptyList(),
			Collections.emptyList(), fields, methods, enclosingType);
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
