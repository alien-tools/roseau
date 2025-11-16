package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class FieldNowFinalRule implements FieldRule {
	@Override
	public void onMatchedField(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		if (!oldField.isFinal() && newField.isFinal()) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_NOW_FINAL, ctx.oldType(), oldField, newField);
		}
	}
}
