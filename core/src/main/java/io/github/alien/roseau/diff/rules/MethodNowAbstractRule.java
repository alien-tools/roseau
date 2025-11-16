package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class MethodNowAbstractRule implements MethodRule {
	@Override
	public void onMatchedMethod(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (!oldMethod.isAbstract() && newMethod.isAbstract()) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_NOW_ABSTRACT, ctx.oldType(), oldMethod, newMethod);
		}
	}
}
