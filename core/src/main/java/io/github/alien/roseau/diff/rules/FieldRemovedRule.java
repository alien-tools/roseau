package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class FieldRemovedRule implements FieldRule {
	@Override
	public void onRemovedField(FieldDecl field, MemberRuleContext ctx) {
		ctx.builder().memberBC(BreakingChangeKind.FIELD_REMOVED, ctx.oldType(), field);
	}
}
