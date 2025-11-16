package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.AnnotationDecl;

public interface AnnotationRule {
	default void onAddedAnnotation(AnnotationDecl annotation, TypeRuleContext ctx) {}
	default void onRemovedAnnotation(AnnotationDecl annotation, TypeRuleContext ctx) {}
	default void onMatchedAnnotation(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation, TypeRuleContext ctx) {}
}
