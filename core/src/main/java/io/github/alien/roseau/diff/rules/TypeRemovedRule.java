package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class TypeRemovedRule implements TypeRule {
	@Override
	public void onRemovedType(TypeDecl type, TypeRuleContext ctx) {
		ctx.builder().typeBC(BreakingChangeKind.TYPE_REMOVED, type);
	}
}
