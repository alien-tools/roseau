package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class FieldNoLongerStaticRule implements FieldRule {
	@Override
	public void onMatchedField(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		if (oldField.isStatic() && !newField.isStatic()) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_NO_LONGER_STATIC, ctx.oldType(), oldField, newField);
		}
	}
}
