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

		// §4.8: existing clients see the erasure of the new return type, even if its declaration did not change
		boolean isErased = ctx.isErasedForExistingClients(newMethod);
		if (!isErased && oldMethod.getType().equals(newMethod.getType())) {
			return;
		}

		// Build combined type-level + method-level mappings. Method-level entries take precedence (shadowing).
		TypeParameterMapping.Normalizer normalizer = TypeParameterMapping.Normalizer.forExecutable(
			ctx.oldType(), ctx.newType(), oldMethod, newMethod);
		ITypeReference normalizedOld = normalizer.normalizeOld(oldMethod.getType());
		ITypeReference normalizedNew = isErased ? newErased : normalizer.normalizeNew(newMethod.getType());

		if (normalizedOld.equals(normalizedNew)) {
			return;
		}

		// An erased return type is assignable to the old one, but its type arguments and type variables are lost
		boolean invokerCompatible = (!isErased || oldMethod.getType().equals(oldErased)) &&
			v2.isSourceCompatibleExpression(newMethod, normalizedOld, normalizedNew);
		boolean overriderCompatible = v1.isEffectivelyFinal(ctx.oldType(), oldMethod) ||
			v2.isSubtypeOf(newMethod, normalizedOld, normalizedNew);
		if (!invokerCompatible || !overriderCompatible) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE, ctx.oldType(), oldMethod,
				newMethod, new BreakingChangeDetails.MethodReturnTypeChanged(oldMethod.getType(),
					isErased ? newErased : newMethod.getType()));
		}
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
