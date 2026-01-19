package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class ConstructorNowProtected implements MemberRule<ConstructorDecl> {
	@Override
	public void onMatched(ConstructorDecl oldCons, ConstructorDecl newCons, MemberRuleContext ctx) {
		if (oldCons.isPublic() && newCons.isProtected()) {
			ctx.builder().memberBC(BreakingChangeKind.CONSTRUCTOR_NOW_PROTECTED, ctx.oldType(), oldCons, newCons);
		}
	}
}
