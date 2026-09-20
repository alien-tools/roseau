package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Map;

/**
 * An annotation on a {@link Symbol}. Repeated annotations use their containing annotation,
 * matching the bytecode representation. Array values retain their ordered elements in braces,
 * and nested annotations use {@code @Type(member=value)} with members sorted by name.
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
		Preconditions.checkNotNull(values);
		// Member names are always present; values may legitimately be empty
		Preconditions.checkArgument(values.entrySet().stream().noneMatch(e ->
			Strings.isNullOrEmpty(e.getKey()) || e.getValue() == null));
		this.actualAnnotation = actualAnnotation;
		this.values = Map.copyOf(values);
	}

	public Annotation(TypeReference<AnnotationDecl> actualAnnotation) {
		this(actualAnnotation, Map.of());
	}

	public boolean hasValues(Map<String, String> expectedValues) {
		return values.entrySet().containsAll(expectedValues.entrySet());
	}
}
