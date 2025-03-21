package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Objects;

/**
 * An annotation on a {@link Symbol}.
 *
 * @param actualAnnotation This annotation's annotation declaration
 */
public record Annotation(TypeReference<AnnotationDecl> actualAnnotation) implements DeepCopyable<Annotation> {
	public Annotation {
		Objects.requireNonNull(actualAnnotation);
	}

	@Override
	public Annotation deepCopy() {
		return new Annotation(actualAnnotation.deepCopy());
	}
}
