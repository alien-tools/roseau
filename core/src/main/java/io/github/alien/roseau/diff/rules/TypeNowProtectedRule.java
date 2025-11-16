package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class TypeNowProtectedRule implements TypeRule {
	@Override
	public void onMatchedType(TypeDecl oldType, TypeDecl newType, TypeRuleContext ctx) {
		if (oldType.isPublic() && newType.isProtected()) {
			ctx.builder().typeBC(BreakingChangeKind.TYPE_NOW_PROTECTED, oldType);
		}
	}
}
