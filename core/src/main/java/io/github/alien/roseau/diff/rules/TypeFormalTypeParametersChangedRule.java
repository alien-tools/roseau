package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class TypeFormalTypeParametersChangedRule implements TypeRule {
	@Override
	public void onMatchedType(TypeDecl oldType, TypeDecl newType, TypeRuleContext ctx) {
		int paramsCount1 = oldType.getFormalTypeParameters().size();
		int paramsCount2 = newType.getFormalTypeParameters().size();

		// Removing formal type parameters always breaks
		if (paramsCount1 > paramsCount2) {
			oldType.getFormalTypeParameters().subList(paramsCount2, paramsCount1)
				.forEach(ftp ->
					ctx.builder().typeBC(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, oldType,
						new BreakingChangeDetails.TypeFormalTypeParametersRemoved(ftp)));
			return;
		}

		// Adding formal type parameters breaks unless it's the first
		if (paramsCount2 > paramsCount1 && paramsCount1 > 0) {
			newType.getFormalTypeParameters().subList(paramsCount1, paramsCount2)
				.forEach(ftp ->
					ctx.builder().typeBC(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, oldType,
						new BreakingChangeDetails.TypeFormalTypeParametersAdded(ftp)));
			return;
		}

		for (int i = 0; i < paramsCount1; i++) {
			FormalTypeParameter p1 = oldType.getFormalTypeParameters().get(i);
			FormalTypeParameter p2 = newType.getFormalTypeParameters().get(i);

			// Each bound in the new version should be a supertype (inclusive) of an existing one
			// so that the type constraints imposed by p1 are stricter than those imposed by p2
			if (p2.bounds().stream()
				.anyMatch(b2 -> !b2.equals(TypeReference.OBJECT) &&
					p1.bounds().stream().noneMatch(b1 -> ctx.v2().isSubtypeOf(b1, b2)))) {
				ctx.builder().typeBC(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, oldType,
					new BreakingChangeDetails.TypeFormalTypeParametersChanged(p1, p2));
			}
		}
	}
}
