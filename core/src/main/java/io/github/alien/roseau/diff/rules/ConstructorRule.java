package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.TypeDecl;

public interface ConstructorRule {
	default void onAddedConstructor(ConstructorDecl cons, MemberRuleContext ctx) {}
	default void onRemovedConstructor(ConstructorDecl cons, MemberRuleContext ctx) {}
	default void onMatchedConstructor(ConstructorDecl oldCons, ConstructorDecl newCons, MemberRuleContext ctx) {}
}
