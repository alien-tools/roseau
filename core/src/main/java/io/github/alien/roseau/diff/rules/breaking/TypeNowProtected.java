package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.TypeRule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class TypeNowProtected implements TypeRule<TypeDecl> {
	@Override
	public void onMatched(TypeDecl oldType, TypeDecl newType, TypeRuleContext ctx) {
		if (oldType.isPublic() && newType.isProtected()) {
			ctx.builder().typeBC(BreakingChangeKind.TYPE_NOW_PROTECTED, oldType);
		}
	}
}
