package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.Rule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class ClassNowCheckedExceptionRule implements Rule<ClassDecl> {
	@Override
	public void onMatched(ClassDecl oldCls, ClassDecl newCls, TypeRuleContext ctx) {
		if (ctx.v1().isUncheckedException(oldCls) && ctx.v2().isCheckedException(newCls)) {
			ctx.builder().typeBC(BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, oldCls);
		}
	}
}
