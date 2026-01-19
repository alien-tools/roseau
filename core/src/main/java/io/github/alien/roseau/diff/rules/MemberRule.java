package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.TypeMemberDecl;

public interface MemberRule<T extends TypeMemberDecl> {
	default void onAdded(T type, MemberRuleContext context) {}
	default void onRemoved(T type, MemberRuleContext context) {}
	default void onMatched(T oldType, T newType, MemberRuleContext context) {}
}
