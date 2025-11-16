package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class ClassNowFinalRule implements ClassRule {
	@Override
	public void onMatchedClass(ClassDecl oldCls, ClassDecl newCls, TypeRuleContext ctx) {
		if (!ctx.v1().isEffectivelyFinal(oldCls) && ctx.v2().isEffectivelyFinal(newCls)) {
			ctx.builder().typeBC(BreakingChangeKind.CLASS_NOW_FINAL, oldCls);
		}
	}
}
