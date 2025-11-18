package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class FieldNowStatic implements MemberRule<FieldDecl> {
	@Override
	public void onMatched(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		if (!oldField.isStatic() && newField.isStatic()) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_NOW_STATIC, ctx.oldType(), oldField, newField);
		}
	}
}
