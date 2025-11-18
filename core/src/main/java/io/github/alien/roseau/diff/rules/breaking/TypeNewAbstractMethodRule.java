package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRuleContext;
import io.github.alien.roseau.diff.rules.MethodRule;

public class TypeNewAbstractMethodRule implements MethodRule {
	@Override
	public void onAddedMethod(MethodDecl method, MemberRuleContext ctx) {
		if (method.isAbstract()) {
				ctx.builder().typeBC(BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, ctx.oldType(),
					new BreakingChangeDetails.TypeNewAbstractMethod(method));
		}
	}
}
