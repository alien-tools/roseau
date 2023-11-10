package com.github.maracas.roseau.model;

import java.util.Collections;
import java.util.List;

public final class AnnotationDecl extends TypeDecl {
	public AnnotationDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, String position, TypeReference containingType, List<FieldDecl> fields, List<MethodDecl> methods) {
		super(qualifiedName, visibility, modifiers, position, containingType, Collections.emptyList(), Collections.emptyList(), fields, methods);
	}

	@Override
	public boolean isAnnotation() {
		return true;
	}

	@Override
	public String toString() {
		return """
			Annotation %s [%s] [%s]
				Containing type: %s
			  Position: %s
			  Fields: %s
			  Methods: %s
			""".formatted(qualifiedName, visibility, modifiers, containingType, position, fields, methods);
	}
}
