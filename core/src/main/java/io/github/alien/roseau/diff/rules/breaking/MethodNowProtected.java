package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class MethodNowProtected implements MemberRule<MethodDecl> {
	@Override
	public void onMatched(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (oldMethod.isPublic() && newMethod.isProtected()) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_NOW_PROTECTED, ctx.oldType(), oldMethod, newMethod);
		}
	}
}
