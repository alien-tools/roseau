package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.analysis.ApiAnalyzer;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

/**
 * Matched methods share their erasure as seen from their type, but clients link against the erasure of their
 * declaration (JLS §13.1), which may differ. For instance, {@code put(T)} inherited from {@code Base<Integer>} keeps
 * its source signature {@code put(Integer)} when {@code T}'s bound changes from {@code Number} to {@code Object}, but
 * its descriptor changes from {@code put(Number)} to {@code put(Object)}.
 */
public class MethodParameterErasureChanged implements MemberRule<MethodDecl> {
	@Override
	public void onMatched(MethodDecl oldMethod, MethodDecl newMethod, MemberRuleContext ctx) {
		ApiAnalyzer v1 = ctx.v1().analyzer();
		ApiAnalyzer v2 = ctx.v2().analyzer();
		String oldErasure = v1.getErasure(v1.getDeclaration(ctx.oldType(), oldMethod));
		String newErasure = v2.getErasure(v2.getDeclaration(ctx.newType(), newMethod));
		if (!oldErasure.equals(newErasure) && v2.getLinkableMethodDeclarations(ctx.newType(), oldErasure).isEmpty()) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_PARAMETER_ERASURE_CHANGED, ctx.oldType(), oldMethod,
				newMethod, new BreakingChangeDetails.MethodParameterErasureChanged(oldErasure, newErasure));
		}
	}
}
