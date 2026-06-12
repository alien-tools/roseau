package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;
import io.github.alien.roseau.diff.rules.TypeRule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class MethodNowAbstract implements MemberRule<MethodDecl>, TypeRule<ClassDecl> {
	@Override
	public void onMatched(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (!oldMethod.isAbstract() && newMethod.isAbstract() &&
			ctx.v1().analyzer().canHaveConcreteSubtypes(ctx.oldType())) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_NOW_ABSTRACT, ctx.oldType(), oldMethod, newMethod);
		}
	}

	@Override
	public void onMatched(ClassDecl oldCls, ClassDecl newCls, TypeRuleContext ctx) {
		if (!ctx.v1().analyzer().canHaveConcreteSubtypes(oldCls) ||
			ctx.v2().analyzer().canHaveConcreteSubtypes(newCls)) {
			return;
		}

		// Report only blockers that were concrete in the old version: an inaccessible method turned abstract. Newly
		// introduced abstract obligations are handled by TypeNewAbstractMethod.
		for (MethodDecl blocker : ctx.v2().analyzer().getConcreteSubtypeBlockers(newCls)) {
			ctx.v1().analyzer()
				.findInheritableMethod(oldCls, blocker.getQualifiedName(), ctx.v2().analyzer().getErasure(blocker))
				.filter(oldMethod -> !oldMethod.isAbstract())
				.ifPresent(oldMethod ->
					ctx.builder().memberBC(BreakingChangeKind.METHOD_NOW_ABSTRACT, oldCls, oldMethod, blocker));
		}
	}
}
