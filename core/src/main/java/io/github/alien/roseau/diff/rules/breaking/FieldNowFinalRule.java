package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class FieldNowFinalRule implements MemberRule<FieldDecl> {
	@Override
	public void onMatched(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		if (!oldField.isFinal() && newField.isFinal()) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_NOW_FINAL, ctx.oldType(), oldField, newField);
		}
	}
}
