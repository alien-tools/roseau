package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

import java.util.HashSet;
import java.util.List;

public class ExecutableFormalTypeParametersChangedRule implements MemberRule<ExecutableDecl> {
	@Override
	public void onMatched(ExecutableDecl oldExecutable, ExecutableDecl newExecutable, MemberRuleContext ctx) {
		int paramsCount1 = oldExecutable.getFormalTypeParameters().size();
		int paramsCount2 = newExecutable.getFormalTypeParameters().size();
		boolean isOverridable = !ctx.v1().isEffectivelyFinal(ctx.oldType(), oldExecutable);

		// Removing a type parameter is breaking if:
		//  - it's a method (due to @Override)
		//  - it's a constructor and there was more than one
		if (paramsCount1 > paramsCount2 && (isOverridable || paramsCount1 > 1)) {
			oldExecutable.getFormalTypeParameters().subList(paramsCount2, paramsCount1)
				.forEach(ftp ->
					ctx.builder().memberBC(BreakingChangeKind.FORMAL_TYPE_PARAMETER_REMOVED, ctx.oldType(), oldExecutable, newExecutable,
						new BreakingChangeDetails.FormalTypeParametersRemoved(ftp)));
			return;
		}

		// Adding a type parameter is only breaking if there was already some
		if (paramsCount1 > 0 && paramsCount1 < paramsCount2) {
			newExecutable.getFormalTypeParameters().subList(paramsCount1, paramsCount2)
				.forEach(ftp ->
					ctx.builder().memberBC(BreakingChangeKind.FORMAL_TYPE_PARAMETER_ADDED, ctx.oldType(), oldExecutable, newExecutable,
						new BreakingChangeDetails.FormalTypeParametersAdded(ftp)));
			return;
		}

		for (int i = 0; i < paramsCount1; i++) {
			FormalTypeParameter ftp1 = oldExecutable.getFormalTypeParameters().get(i);
			List<ITypeReference> bounds1 = ftp1.bounds();

			if (i < paramsCount2) {
				FormalTypeParameter ftp2 = newExecutable.getFormalTypeParameters().get(i);
				List<ITypeReference> bounds2 = ftp2.bounds();

				if (isOverridable) { // Invariant
					if (!new HashSet<>(bounds1).equals(new HashSet<>(bounds2))) {
						ctx.builder().memberBC(BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, ctx.oldType(), oldExecutable, newExecutable,
							new BreakingChangeDetails.FormalTypeParametersChanged(ftp1, ftp2));
					}
				} else { // Variance
					// Any new bound that's not a supertype of an existing bound is breaking
					if (bounds2.stream()
						// We can safely ignore this bound
						.filter(b2 -> !b2.equals(TypeReference.OBJECT))
						.anyMatch(b2 -> bounds1.stream().noneMatch(b1 -> ctx.v1().isSubtypeOf(b1, b2)))) {
						ctx.builder().memberBC(BreakingChangeKind.FORMAL_TYPE_PARAMETER_CHANGED, ctx.oldType(), oldExecutable, newExecutable,
							new BreakingChangeDetails.FormalTypeParametersChanged(ftp1, ftp2));
					}
				}
			}
		}
	}
}
