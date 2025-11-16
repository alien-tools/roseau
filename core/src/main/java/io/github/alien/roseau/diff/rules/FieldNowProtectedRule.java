package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class FieldNowProtectedRule implements FieldRule {
	@Override
	public void onMatchedField(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		if (oldField.isPublic() && newField.isProtected()) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_NOW_PROTECTED, ctx.oldType(), oldField, newField);
		}
	}
}
