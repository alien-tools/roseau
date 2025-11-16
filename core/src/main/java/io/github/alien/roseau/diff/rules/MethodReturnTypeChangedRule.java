package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class MethodReturnTypeChangedRule implements MethodRule {
	@Override
	public void onMatchedMethod(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (!oldMethod.getType().equals(newMethod.getType())) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, ctx.oldType(), oldMethod, newMethod,
				new BreakingChangeDetails.MethodReturnTypeChanged(oldMethod.getType(), newMethod.getType()));
		}
	}
}
