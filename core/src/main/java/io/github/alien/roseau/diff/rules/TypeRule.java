package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.TypeDecl;

public interface TypeRule {
	default void onAddedType(TypeDecl type, TypeRuleContext ctx) {}
	default void onRemovedType(TypeDecl type, TypeRuleContext ctx) {}
	default void onMatchedType(TypeDecl oldType, TypeDecl newType, TypeRuleContext ctx) {}
}
