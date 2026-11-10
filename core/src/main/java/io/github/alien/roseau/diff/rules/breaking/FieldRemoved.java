package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class FieldRemoved implements MemberRule<FieldDecl> {
	@Override
	public void onRemoved(FieldDecl field, MemberRuleContext ctx) {
		BreakingChangeKind kind = field.isCompileTimeConstant()
			? BreakingChangeKind.FIELD_CONSTANT_REMOVED
			: BreakingChangeKind.FIELD_REMOVED;
		ctx.builder().memberBC(kind, ctx.oldType(), field);
	}
}
