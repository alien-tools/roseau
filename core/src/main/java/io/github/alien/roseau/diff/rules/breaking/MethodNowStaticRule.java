package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class MethodNowStaticRule implements MemberRule<MethodDecl> {
	@Override
	public void onMatched(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (!oldMethod.isStatic() && newMethod.isStatic()) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_NOW_STATIC, ctx.oldType(), oldMethod, newMethod);
		}
	}
}
