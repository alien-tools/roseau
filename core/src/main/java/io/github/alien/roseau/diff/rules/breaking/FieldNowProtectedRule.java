package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.FieldRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class FieldNowProtectedRule implements FieldRule {
	@Override
	public void onMatchedField(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		if (oldField.isPublic() && newField.isProtected()) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_NOW_PROTECTED, ctx.oldType(), oldField, newField);
		}
	}
}
