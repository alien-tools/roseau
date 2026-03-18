package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.analysis.TypeParameterMapping;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
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
					ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED,
						ctx.oldType(), oldExecutable, newExecutable, details);
					return;
				}

				TypeParameterMapping.Normalizer normalizer = TypeParameterMapping.Normalizer.forExecutable(
					ctx.oldType(), ctx.newType(), oldExecutable, newExecutable);
				TypeReference<?> normalizedPt1 = (TypeReference<?>) normalizer.normalizeOld(pt1);

				boolean isFinalExecutable = ctx.v1().isEffectivelyFinal(ctx.oldType(), oldExecutable);

				// Can be overridden = invariant: only rename old type params (do NOT erase new type params, since
				// generizing a type argument like List<Object> → List<T> IS a source break due to name clashes
				// in overriding subclasses)
				if (!isFinalExecutable && !normalizedPt1.equals(pt2)) {
					ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED,
						ctx.oldType(), oldExecutable, newExecutable, details);
				}

				// Can't be overridden = variance: also erase newly added type params
				if (isFinalExecutable) {
					TypeReference<?> normalizedPt2 = (TypeReference<?>) normalizer.normalizeNew(pt2);
					if (!ctx.v2().isSubtypeOf(newExecutable, normalizedPt1, normalizedPt2)) {
						ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED,
							ctx.oldType(), oldExecutable, newExecutable, details);
					}
				}
			}
		}
	}
}
