package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class MethodNowFinalRule implements MemberRule<MethodDecl> {
	@Override
	public void onMatched(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (!ctx.v1().isEffectivelyFinal(ctx.oldType(), oldMethod) && ctx.v2().isEffectivelyFinal(ctx.newType(), newMethod)) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_NOW_FINAL, ctx.oldType(), oldMethod, newMethod);
		}
	}
}
