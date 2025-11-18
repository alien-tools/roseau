package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class MethodReturnTypeChangedRule implements MemberRule<MethodDecl> {
	@Override
	public void onMatched(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (!oldMethod.getType().equals(newMethod.getType())) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, ctx.oldType(), oldMethod, newMethod,
				new BreakingChangeDetails.MethodReturnTypeChanged(oldMethod.getType(), newMethod.getType()));
		}
	}
}
