package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.analysis.TypeParameterMapping;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class FieldTypeChanged implements MemberRule<FieldDecl> {
	@Override
	public void onMatched(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		// Normalize type references across versions to handle type parameter renaming (T→U) and generization
		// (Object→T). The forward mapping renames old type params to v2's names; the eraseAdded mapping replaces
		// newly added type params with their erasure (raw-type semantics for existing clients).
		TypeParameterMapping.Normalizer normalizer = TypeParameterMapping.Normalizer.forType(ctx.oldType(), ctx.newType());
		ITypeReference normalizedOld = normalizer.normalizeOld(oldField.getType());
		ITypeReference normalizedNew = normalizer.normalizeNew(newField.getType());

		BreakingChangeDetails details = new BreakingChangeDetails.FieldTypeChanged(oldField.getType(), newField.getType());

		ITypeReference oldErased = ctx.v1().erasure().getErasedType(ctx.oldType(), oldField.getType());
		ITypeReference newErased = ctx.v2().erasure().getErasedType(ctx.newType(), newField.getType());
		if (!oldErased.equals(newErased) && !oldField.isCompileTimeConstant()) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED,
				ctx.oldType(), oldField, newField, details);
		}

		boolean readCompatible = ctx.v2().isExpressionCompatible(ctx.newType(), normalizedOld, normalizedNew);
		boolean writeCompatible = oldField.isFinal() ||
			ctx.v2().isAssignmentCompatible(ctx.newType(), normalizedOld, normalizedNew);

		if (!readCompatible || !writeCompatible) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_TYPE_CHANGED_INCOMPATIBLE,
				ctx.oldType(), oldField, newField, details);
		}
	}
}
