package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class MethodNoLongerStaticRule implements MethodRule {
	@Override
	public void onMatchedMethod(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (oldMethod.isStatic() && !newMethod.isStatic()) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_NO_LONGER_STATIC, ctx.oldType(), oldMethod, newMethod);
		}
	}
}
