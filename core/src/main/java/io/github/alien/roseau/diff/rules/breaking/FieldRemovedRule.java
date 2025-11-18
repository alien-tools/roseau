package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class FieldRemovedRule implements MemberRule<FieldDecl> {
	@Override
	public void onRemoved(FieldDecl field, MemberRuleContext ctx) {
		ctx.builder().memberBC(BreakingChangeKind.FIELD_REMOVED, ctx.oldType(), field);
	}
}
