package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class AnnotationDecl extends TypeDecl {
	public AnnotationDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                      List<Annotation> annotations, SourceLocation location, List<FieldDecl> fields,
	                      List<MethodDecl> methods, TypeReference<TypeDecl> enclosingType) {
		super(qualifiedName, visibility, modifiers, annotations, location, Collections.emptyList(),
			Collections.emptyList(), fields, methods, enclosingType);
	}

	@Override
	public boolean isAnnotation() {
		return true;
	}

	@Override
	public String toString() {
		return """
			%s annotation %s
			  %s
			  %s
			""".formatted(visibility, qualifiedName, fields, methods);
	}
}
