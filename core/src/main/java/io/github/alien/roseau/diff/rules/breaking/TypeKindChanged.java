package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.TypeRule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class TypeKindChanged implements TypeRule<TypeDecl> {
	@Override
	public void onMatched(TypeDecl oldType, TypeDecl newType, TypeRuleContext ctx) {
		ctx.builder().typeBC(BreakingChangeKind.TYPE_KIND_CHANGED, oldType,
			new BreakingChangeDetails.TypeKindChanged(oldType.getClass(), newType.getClass()));
	}
}
