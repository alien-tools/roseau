package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class TypeKindChangedRule implements TypeRule {
	@Override
	public void onMatchedType(TypeDecl oldType, TypeDecl newType, TypeRuleContext ctx) {
		if (!oldType.getClass().equals(newType.getClass())) {
			ctx.builder().typeBC(BreakingChangeKind.TYPE_KIND_CHANGED, oldType,
				new BreakingChangeDetails.ClassTypeChanged(oldType.getClass(), newType.getClass()));
		}
	}
}
