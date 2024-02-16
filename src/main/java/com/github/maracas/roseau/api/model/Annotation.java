package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.maracas.roseau.api.model.reference.TypeReference;

public class Annotation {
	private final TypeReference<AnnotationDecl> actualAnnotation;

	@JsonCreator
	public Annotation(TypeReference<AnnotationDecl> actualAnnotation) {
		this.actualAnnotation = actualAnnotation;
	}

	public TypeReference<AnnotationDecl> getActualAnnotation() {
		return actualAnnotation;
	}
}
