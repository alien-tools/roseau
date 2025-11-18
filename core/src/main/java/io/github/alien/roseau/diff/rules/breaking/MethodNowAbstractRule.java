package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRuleContext;
import io.github.alien.roseau.diff.rules.MethodRule;

public class MethodNowAbstractRule implements MethodRule {
	@Override
	public void onMatchedMethod(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (!oldMethod.isAbstract() && newMethod.isAbstract()) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_NOW_ABSTRACT, ctx.oldType(), oldMethod, newMethod);
		}
	}
}
