package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.TypeRule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

import java.util.List;

public class TypeSupertypeRemovedRule implements TypeRule<TypeDecl> {
	// If a supertype that was exported has been removed,
	// it may have been used in client code for casts
	@Override
	public void onMatched(TypeDecl oldType, TypeDecl newType, TypeRuleContext ctx) {
		List<TypeReference<TypeDecl>> candidates = ctx.v1().getAllSuperTypes(oldType).stream()
			.filter(sup -> ctx.v1().isExported(sup))
			.filter(sup -> !ctx.v2().isSubtypeOf(newType, sup))
			.toList();

		// Only report the closest super type
		candidates.stream()
			.filter(sup -> candidates.stream().noneMatch(other -> !other.equals(sup) && ctx.v1().isSubtypeOf(other, sup)))
			.forEach(sup ->
				ctx.builder().typeBC(BreakingChangeKind.TYPE_SUPERTYPE_REMOVED, oldType,
					new BreakingChangeDetails.TypeSupertypeRemoved(sup)));
	}
}
