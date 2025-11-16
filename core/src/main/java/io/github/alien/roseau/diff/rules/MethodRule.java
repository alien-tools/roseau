package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.MethodDecl;

public interface MethodRule {
	default void onAddedMethod(MethodDecl method, MemberRuleContext ctx) {}
	default void onRemovedMethod(MethodDecl method, MemberRuleContext ctx) {}
	default void onMatchedMethod(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {}
}
