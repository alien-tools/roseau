package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.InterfaceDecl;

public interface InterfaceRule {
	default void onAddedInterface(InterfaceDecl intf, TypeRuleContext ctx) {}
	default void onRemovedInterface(InterfaceDecl intf, TypeRuleContext ctx) {}
	default void onMatchedInterface(InterfaceDecl oldIntf, InterfaceDecl newIntf, TypeRuleContext ctx) {}
}
