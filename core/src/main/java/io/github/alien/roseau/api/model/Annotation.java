package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.reference.TypeReference;

/**
 * An annotation on a {@link Symbol}.
 *
 * @param actualAnnotation This annotation's annotation declaration
 */
public record Annotation(TypeReference<AnnotationDecl> actualAnnotation) {
	public Annotation {
		Preconditions.checkNotNull(actualAnnotation);
	}
}
