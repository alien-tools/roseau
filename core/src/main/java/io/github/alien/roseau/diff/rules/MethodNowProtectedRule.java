package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class MethodNowProtectedRule implements MethodRule {
	@Override
	public void onMatchedMethod(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (oldMethod.isPublic() && newMethod.isProtected()) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_NOW_PROTECTED, ctx.oldType(), oldMethod, newMethod);
		}
	}
}
