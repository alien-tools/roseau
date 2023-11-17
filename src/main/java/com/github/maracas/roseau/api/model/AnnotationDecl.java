package com.github.maracas.roseau.api.model;

import java.util.Collections;
import java.util.List;

public final class AnnotationDecl extends TypeDecl {
	public AnnotationDecl(String qualifiedName, AccessModifier visibility, boolean isExported, List<Modifier> modifiers, String position, TypeReference<TypeDecl> containingType, List<FieldDecl> fields, List<MethodDecl> methods) {
		super(qualifiedName, visibility, isExported, modifiers, position, containingType, Collections.emptyList(), Collections.emptyList(), fields, methods);
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
