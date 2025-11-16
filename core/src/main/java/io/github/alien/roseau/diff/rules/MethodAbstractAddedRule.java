package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class MethodAbstractAddedRule implements MethodRule {
	@Override
	public void onAddedMethod(MethodDecl method, MemberRuleContext ctx) {
		if (method.isAbstract()) {
			if (ctx.oldType().isInterface()) {
				ctx.builder().typeBC(BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, ctx.oldType(),
					new BreakingChangeDetails.MethodAddedToInterface(method));
			}

			if (ctx.oldType().isClass()) {
				ctx.builder().typeBC(BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, ctx.oldType(),
					new BreakingChangeDetails.MethodAbstractAddedToClass(method));
			}
		}
	}
}
