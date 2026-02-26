package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.TypeParameterScope;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class ExecutableParameterGenericsChanged implements MemberRule<ExecutableDecl> {
	@Override
	public void onMatched(ExecutableDecl oldExecutable, ExecutableDecl newExecutable, MemberRuleContext ctx) {
		// We checked executable erasures, so we know parameter types are equals modulo type arguments
		for (int i = 0; i < oldExecutable.getParameters().size(); i++) {
			ParameterDecl p1 = oldExecutable.getParameters().get(i);
			ParameterDecl p2 = newExecutable.getParameters().get(i);

			/*
			 * In general, we need to distinguish how formal type parameters and parameter generics are handled between methods
			 * and constructors: the former can be overridden (thus parameters are immutable so that signatures in
			 * sub/super-classes match), and the latter cannot (thus parameters can follow variance rules).
			 */
			if (p1.type() instanceof TypeReference<?> pt1 && p2.type() instanceof TypeReference<?> pt2) {
				BreakingChangeDetails details =
					new BreakingChangeDetails.MethodParameterGenericsChanged(pt1, pt2);

				if (pt1.typeArguments().size() != pt2.typeArguments().size()) {
					ctx.builder().memberBC(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, ctx.oldType(), oldExecutable, newExecutable, details);
					return;
				}

				boolean isFinalExecutable = ctx.v1().isEffectivelyFinal(ctx.oldType(), oldExecutable);
				// Can be overridden = invariant
				if (!isFinalExecutable && !pt1.equals(pt2)) {
					ctx.builder().memberBC(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, ctx.oldType(), oldExecutable, newExecutable, details);
				}

				// Can't = variance
				if (isFinalExecutable && !ctx.v1().isSubtypeOf(TypeParameterScope.EMPTY, pt1, pt2)) {
					ctx.builder().memberBC(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, ctx.oldType(), oldExecutable, newExecutable, details);
				}
			}
		}
	}
}
