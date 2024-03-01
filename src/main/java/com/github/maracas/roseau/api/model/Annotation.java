package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.TypeReference;

public record Annotation(TypeReference<AnnotationDecl> actualAnnotation) {
}
