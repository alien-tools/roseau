package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Objects;

/**
 * A concrete annotation on a code element
 *
 * @param actualAnnotation The annotation declaration this annotation is an instance of
 */
public record Annotation(TypeReference<AnnotationDecl> actualAnnotation) {
	public Annotation {
		Objects.requireNonNull(actualAnnotation);
	}
}
