package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.Rule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class ClassNowFinalRule implements Rule<ClassDecl> {
	@Override
	public void onMatched(ClassDecl oldCls, ClassDecl newCls, TypeRuleContext ctx) {
		if (!ctx.v1().isEffectivelyFinal(oldCls) && ctx.v2().isEffectivelyFinal(newCls)) {
			ctx.builder().typeBC(BreakingChangeKind.CLASS_NOW_FINAL, oldCls);
		}
	}
}
