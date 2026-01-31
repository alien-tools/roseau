package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class FieldTypeChanged implements MemberRule<FieldDecl> {
	@Override
	public void onMatched(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		if (!oldField.getType().equals(newField.getType())) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_TYPE_CHANGED, ctx.oldType(), oldField, newField,
				new BreakingChangeDetails.FieldTypeChanged(oldField.getType(), newField.getType()));
		}
	}
}
