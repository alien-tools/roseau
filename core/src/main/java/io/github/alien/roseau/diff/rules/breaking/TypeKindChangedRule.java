package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.Rule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class TypeKindChangedRule implements Rule<TypeDecl> {
	@Override
	public void onMatched(TypeDecl oldType, TypeDecl newType, TypeRuleContext ctx) {
		if (!oldType.getClass().equals(newType.getClass())) {
			ctx.builder().typeBC(BreakingChangeKind.TYPE_KIND_CHANGED, oldType,
				new BreakingChangeDetails.TypeKindChanged(oldType.getClass(), newType.getClass()));
		}
	}
}
