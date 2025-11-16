package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.FieldDecl;

public interface FieldRule {
	default void onAddedField(FieldDecl field, MemberRuleContext ctx) {}
	default void onRemovedField(FieldDecl field, MemberRuleContext ctx) {}
	default void onMatchedField(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {}
}
