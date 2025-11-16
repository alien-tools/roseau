package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.ClassDecl;

public interface ClassRule {
	default void onAddedClass(ClassDecl cls, TypeRuleContext ctx) {}
	default void onRemovedClass(ClassDecl cls, TypeRuleContext ctx) {}
	default void onMatchedClass(ClassDecl oldCls, ClassDecl newCls, TypeRuleContext ctx) {}
}
