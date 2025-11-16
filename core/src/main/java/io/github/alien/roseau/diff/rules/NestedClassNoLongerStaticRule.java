package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class NestedClassNoLongerStaticRule implements ClassRule {
	@Override
	public void onMatchedClass(ClassDecl oldCls, ClassDecl newCls, TypeRuleContext ctx) {
		if (oldCls.isNested() && newCls.isNested() && oldCls.isStatic() && !newCls.isStatic()) {
			ctx.builder().typeBC(BreakingChangeKind.NESTED_CLASS_NO_LONGER_STATIC, oldCls);
		}
	}
}
