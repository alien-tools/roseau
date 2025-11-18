package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.TypeDecl;

public interface Rule<T extends TypeDecl> {
	default void onAdded(T type, TypeRuleContext context) {}
	default void onRemoved(T type, TypeRuleContext context) {}
	default void onMatched(T oldType, T newType, TypeRuleContext context) {}
}
