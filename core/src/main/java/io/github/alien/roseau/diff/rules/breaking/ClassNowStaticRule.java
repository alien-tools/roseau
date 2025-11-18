package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.Rule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class ClassNowStaticRule implements Rule<ClassDecl> {
	@Override
	public void onMatched(ClassDecl oldCls, ClassDecl newCls, TypeRuleContext ctx) {
		if (oldCls.isNested() && newCls.isNested() && !oldCls.isStatic() && newCls.isStatic()) {
			ctx.builder().typeBC(BreakingChangeKind.CLASS_NOW_STATIC, oldCls);
		}
	}
}
