package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.Rule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class ClassNowAbstractRule implements Rule<ClassDecl> {
	@Override
	public void onMatched(ClassDecl oldCls, ClassDecl newCls, TypeRuleContext ctx) {
		if (!oldCls.isEffectivelyAbstract() && newCls.isEffectivelyAbstract()) {
			ctx.builder().typeBC(BreakingChangeKind.CLASS_NOW_ABSTRACT, oldCls);
		}
	}
}
