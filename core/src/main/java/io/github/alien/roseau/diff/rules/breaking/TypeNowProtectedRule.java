package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.Rule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class TypeNowProtectedRule implements Rule<TypeDecl> {
	@Override
	public void onMatched(TypeDecl oldType, TypeDecl newType, TypeRuleContext ctx) {
		if (oldType.isPublic() && newType.isProtected()) {
			ctx.builder().typeBC(BreakingChangeKind.TYPE_NOW_PROTECTED, oldType);
		}
	}
}
