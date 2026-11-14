package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.analysis.ApiAnalyzer;
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
		// Clients link against the erasure of the method's declaration (JLS §13.1), not of the method as seen from the
		// type that supplies type arguments. It may change while the declared type does not (e.g., bound change).
		ApiAnalyzer v1 = ctx.v1().analyzer();
		ApiAnalyzer v2 = ctx.v2().analyzer();
		MethodDecl oldDecl = v1.getDeclaration(ctx.oldType(), oldMethod);
		MethodDecl newDecl = v2.getDeclaration(ctx.newType(), newMethod);
		ITypeReference oldErased = v1.getErasedType(oldDecl, oldDecl.getType());
		ITypeReference newErased = v2.getErasedType(newDecl, newDecl.getType());
		if (!oldErased.equals(newErased) && !isLinkable(oldDecl, oldErased, ctx)) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_ERASURE_CHANGED, ctx.oldType(), oldMethod,
				newMethod, new BreakingChangeDetails.MethodReturnTypeChanged(oldErased, newErased));
		}

		// Clients that parameterize the type see the method as it is declared, and those that use the type raw see its
		// erasure (§4.8). The existing clients of a type that becomes generic can only use it raw.
		TypeParameterMapping.Normalizer normalizer = TypeParameterMapping.Normalizer.forExecutable(
			ctx.oldType(), ctx.newType(), oldMethod, newMethod);
		ITypeReference normalizedOld = normalizer.normalizeOld(oldMethod.getType());
		if (!ctx.isErasedForExistingClients(newMethod) &&
			!isCompatible(oldMethod, newMethod, normalizedOld, normalizer.normalizeNew(newMethod.getType()), ctx)) {
			report(oldMethod, newMethod, oldMethod.getType(), newMethod.getType(), ctx);
		} else if (v2.isErasedInRawType(ctx.newType(), newMethod)) {
			boolean wasErased = v1.isErasedInRawType(ctx.oldType(), oldMethod);
			if (!isCompatible(oldMethod, newMethod, wasErased ? oldErased : normalizedOld, newErased, ctx)) {
				report(oldMethod, newMethod, wasErased ? oldErased : oldMethod.getType(), newErased, ctx);
			}
		}
	}

	private static boolean isCompatible(MethodDecl oldMethod, MethodDecl newMethod, ITypeReference oldType,
	                                    ITypeReference newType, MemberRuleContext ctx) {
		if (oldType.equals(newType)) {
			return true;
		}

		ApiAnalyzer v2 = ctx.v2().analyzer();
		boolean invokerCompatible = v2.isSourceCompatibleExpression(newMethod, oldType, newType);
		boolean overriderCompatible = ctx.v1().analyzer().isEffectivelyFinal(ctx.oldType(), oldMethod) ||
			v2.isSubtypeOf(newMethod, oldType, newType);
		return invokerCompatible && overriderCompatible;
	}

	private static void report(MethodDecl oldMethod, MethodDecl newMethod, ITypeReference oldType,
	                           ITypeReference newType, MemberRuleContext ctx) {
		ctx.builder().memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, ctx.oldType(), oldMethod,
			newMethod, new BreakingChangeDetails.MethodReturnTypeChanged(oldType, newType));
	}

	/**
	 * Checks whether a method with the old descriptor remains linkable in the new hierarchy, e.g., an overridden
	 * {@code Supplier.get()Object} next to a new {@code String get()}.
	 */
	private static boolean isLinkable(MethodDecl oldDecl, ITypeReference oldErased, MemberRuleContext ctx) {
		ApiAnalyzer v2 = ctx.v2().analyzer();
		return v2.getLinkableMethodDeclarations(ctx.newType(), ctx.v1().analyzer().getErasure(oldDecl)).stream()
			.anyMatch(decl -> oldErased.equals(v2.getErasedType(decl, decl.getType())));
	}
}
