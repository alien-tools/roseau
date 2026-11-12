package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.analysis.TypeParameterMapping;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class MethodReturnTypeChanged implements MemberRule<MethodDecl> {
	@Override
	public void onMatched(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		// §4.8: existing clients see the erasure of the new return type, even if its declaration did not change
		boolean isErased = ctx.isErasedForExistingClients(newMethod);
		if (!isErased && oldMethod.getType().equals(newMethod.getType())) {
			return;
		}

		ITypeReference oldErased = ctx.v1().analyzer().erasure().getErasedType(oldMethod, oldMethod.getType());
		ITypeReference newErased = ctx.v2().analyzer().erasure().getErasedType(newMethod, newMethod.getType());

		// Build combined type-level + method-level mappings. Method-level entries take precedence (shadowing).
		TypeParameterMapping.Normalizer normalizer = TypeParameterMapping.Normalizer.forExecutable(
			ctx.oldType(), ctx.newType(), oldMethod, newMethod);
		ITypeReference normalizedOld = normalizer.normalizeOld(oldMethod.getType());
		ITypeReference normalizedNew = isErased ? newErased : normalizer.normalizeNew(newMethod.getType());

		if (normalizedOld.equals(normalizedNew)) {
			return;
		}

		BreakingChangeDetails details = new BreakingChangeDetails.MethodReturnTypeChanged(
			oldMethod.getType(), isErased ? newErased : newMethod.getType());

		if (!oldErased.equals(newErased)) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED,
				ctx.oldType(), oldMethod, newMethod, details);
		}

		// An erased return type is assignable to the old one, but its type arguments and type variables are lost
		boolean invokerCompatible = (!isErased || oldMethod.getType().equals(oldErased)) &&
			ctx.v2().analyzer().isSourceCompatibleExpression(newMethod, normalizedOld, normalizedNew);
		boolean overriderCompatible = ctx.v1().analyzer().isEffectivelyFinal(ctx.oldType(), oldMethod) ||
			ctx.v2().analyzer().isSubtypeOf(newMethod, normalizedOld, normalizedNew);
		if (!invokerCompatible || !overriderCompatible) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE,
				ctx.oldType(), oldMethod, newMethod, details);
		}
	}
}
