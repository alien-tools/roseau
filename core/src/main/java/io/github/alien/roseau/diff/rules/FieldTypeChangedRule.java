package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class FieldTypeChangedRule implements FieldRule {
	@Override
	public void onMatchedField(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		if (!oldField.getType().equals(newField.getType())) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_TYPE_CHANGED, ctx.oldType(), oldField, newField,
				new BreakingChangeDetails.FieldTypeChanged(oldField.getType(), newField.getType()));
		}
	}
}
