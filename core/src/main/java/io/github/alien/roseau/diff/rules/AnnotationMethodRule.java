package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.TypeDecl;

public interface AnnotationMethodRule {
	default void onAddedAnnotationMethod(AnnotationMethodDecl method, MemberRuleContext ctx) {}
	default void onRemovedAnnotationMethod(AnnotationMethodDecl method, MemberRuleContext ctx) {}
	default void onMatchedAnnotationMethod(AnnotationMethodDecl oldMethod, AnnotationMethodDecl newMethod,
	                                       MemberRuleContext ctx) {}
}
