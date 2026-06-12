package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;
import io.github.alien.roseau.diff.rules.TypeRule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class TypeNewAbstractMethod implements MemberRule<MethodDecl>, TypeRule<ClassDecl> {
	@Override
	public void onAdded(MethodDecl method, MemberRuleContext ctx) {
		if (method.isAbstract() && ctx.v1().analyzer().canHaveConcreteSubtypes(ctx.oldType())) {
			ctx.builder().typeBC(BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, ctx.oldType(), method,
				new BreakingChangeDetails.TypeNewAbstractMethod(method));
		}
	}

	@Override
	public void onMatched(ClassDecl oldCls, ClassDecl newCls, TypeRuleContext ctx) {
		if (!ctx.v1().analyzer().canHaveConcreteSubtypes(oldCls) ||
			ctx.v2().analyzer().canHaveConcreteSubtypes(newCls)) {
			return;
		}

		// New blockers either appeared (no matching old method) or were already abstract and lost the concrete library
		// override that satisfied them. A blocker that was concrete in the old version is handled by MethodNowAbstract.
		for (MethodDecl blocker : ctx.v2().analyzer().getConcreteSubtypeBlockers(newCls)) {
			boolean wasConcrete = ctx.v1().analyzer()
				.findInheritableMethod(oldCls, blocker.getQualifiedName(), ctx.v2().analyzer().getErasure(blocker))
				.map(old -> !old.isAbstract())
				.orElse(false);
			if (!wasConcrete) {
				ctx.builder().typeBC(BreakingChangeKind.TYPE_NEW_ABSTRACT_METHOD, oldCls, blocker,
					new BreakingChangeDetails.TypeNewAbstractMethod(blocker));
			}
		}
	}
}
