package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Map;

/**
 * An annotation on a {@link Symbol}.
 *
 * @param actualAnnotation This annotation's annotation declaration
 * @param values           Key-value string-based representation of annotation values
 */
public record Annotation(
	TypeReference<AnnotationDecl> actualAnnotation,
	Map<String, String> values
) {
	public Annotation(TypeReference<AnnotationDecl> actualAnnotation, Map<String, String> values) {
		Preconditions.checkNotNull(actualAnnotation);
		Preconditions.checkArgument(values != null &&
			values.keySet().stream().noneMatch(Strings::isNullOrEmpty));
		this.actualAnnotation = actualAnnotation;
		this.values = Map.copyOf(values);
	}

	public Annotation(TypeReference<AnnotationDecl> actualAnnotation) {
		this(actualAnnotation, Map.of());
	}
}
