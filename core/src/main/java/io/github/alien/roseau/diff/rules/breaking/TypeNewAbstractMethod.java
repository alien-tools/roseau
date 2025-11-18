package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class TypeNewAbstractMethod implements MemberRule<MethodDecl> {
	@Override
	public void onAdded(MethodDecl method, MemberRuleContext ctx) {
		if (method.isAbstract()) {
			ctx.builder().typeBC(BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, ctx.oldType(),
				new BreakingChangeDetails.TypeNewAbstractMethod(method));
		}
	}
}
