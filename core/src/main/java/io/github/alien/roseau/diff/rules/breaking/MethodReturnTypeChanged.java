package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class MethodReturnTypeChanged implements MemberRule<MethodDecl> {
	@Override
	public void onMatched(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		if (oldMethod.getType().equals(newMethod.getType())) {
			return;
		}

		BreakingChangeDetails details = new BreakingChangeDetails.MethodReturnTypeChanged(
			oldMethod.getType(), newMethod.getType());

		ITypeReference oldErased = ctx.v1().erasure().getErasedType(oldMethod, oldMethod.getType());
		ITypeReference newErased = ctx.v2().erasure().getErasedType(newMethod, newMethod.getType());
		if (!oldErased.equals(newErased)) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED,
				ctx.oldType(), oldMethod, newMethod, details);
		}

		boolean invokerCompatible = ctx.v2().isAssignable(newMethod, newMethod.getType(), oldMethod.getType());
		boolean overriderCompatible = ctx.v1().isEffectivelyFinal(ctx.oldType(), oldMethod) ||
			ctx.v2().isSubtypeOf(newMethod, oldMethod.getType(), newMethod.getType());
		if (!invokerCompatible || !overriderCompatible) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE,
				ctx.oldType(), oldMethod, newMethod, details);
		}
	}
}
