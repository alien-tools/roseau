package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class MethodNowFinalRule implements MethodRule {
	@Override
	public void onMatchedMethod(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (!ctx.v1().isEffectivelyFinal(ctx.oldType(), oldMethod) && ctx.v2().isEffectivelyFinal(ctx.newType(), newMethod)) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_NOW_FINAL, ctx.oldType(), oldMethod, newMethod);
		}
	}
}
