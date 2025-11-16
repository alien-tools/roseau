package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.ExecutableDecl;

public interface ExecutableRule {
	default void onAddedExecutable(ExecutableDecl executable, MemberRuleContext ctx) {}
	default void onRemovedExecutable(ExecutableDecl executable, MemberRuleContext ctx) {}
	default void onMatchedExecutable(ExecutableDecl oldExecutable, ExecutableDecl newExecutable, MemberRuleContext ctx) {}
}
