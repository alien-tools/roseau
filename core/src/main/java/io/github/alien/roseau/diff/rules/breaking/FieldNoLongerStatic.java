package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class FieldNoLongerStatic implements MemberRule<FieldDecl> {
	@Override
	public void onMatched(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		if (oldField.isStatic() && !newField.isStatic()) {
			BreakingChangeKind kind = oldField.isCompileTimeConstant()
				? BreakingChangeKind.FIELD_CONSTANT_NO_LONGER_STATIC
				: BreakingChangeKind.FIELD_NO_LONGER_STATIC;
			ctx.builder().memberBC(kind, ctx.oldType(), oldField, newField);
		}
	}
}
