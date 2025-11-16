package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.EnumDecl;

public interface EnumRule {
	default void onAddedEnum(EnumDecl enm, TypeRuleContext ctx) {}
	default void onRemovedEnum(EnumDecl enm, TypeRuleContext ctx) {}
	default void onMatchedEnum(EnumDecl oldEnum, EnumDecl newEnum, TypeRuleContext ctx) {}
}
