package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class ClassNowAbstractRule implements ClassRule {
	@Override
	public void onMatchedClass(ClassDecl oldCls, ClassDecl newCls, TypeRuleContext ctx) {
		if (!oldCls.isEffectivelyAbstract() && newCls.isEffectivelyAbstract()) {
			ctx.builder().typeBC(BreakingChangeKind.CLASS_NOW_ABSTRACT, oldCls);
		}
	}
}
