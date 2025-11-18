package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.TypeRule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class TypeRemovedRule implements TypeRule<TypeDecl> {
	@Override
	public void onRemoved(TypeDecl type, TypeRuleContext ctx) {
		ctx.builder().typeBC(BreakingChangeKind.TYPE_REMOVED, type);
	}
}
