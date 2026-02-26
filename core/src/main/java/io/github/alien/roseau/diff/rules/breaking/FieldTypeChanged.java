package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class FieldTypeChanged implements MemberRule<FieldDecl> {
	@Override
	public void onMatched(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		if (oldField.getType().equals(newField.getType())) {
			return;
		}

		BreakingChangeDetails.FieldTypeChanged details =
			new BreakingChangeDetails.FieldTypeChanged(oldField.getType(), newField.getType());

		ITypeReference oldErased = ctx.v1().erasure().getErasedType(ctx.oldType(), oldField.getType());
		ITypeReference newErased = ctx.v2().erasure().getErasedType(ctx.newType(), newField.getType());
		if (!oldErased.equals(newErased) && !oldField.isCompileTimeConstant()) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED,
				ctx.oldType(), oldField, newField, details);
		}

		boolean readCompatible = ctx.v2().isAssignable(ctx.newType(), newField.getType(), oldField.getType());
		boolean writeCompatible = oldField.isFinal() ||
			ctx.v2().isAssignable(ctx.newType(), oldField.getType(), newField.getType());

		if (!readCompatible || !writeCompatible) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_TYPE_CHANGED_INCOMPATIBLE,
				ctx.oldType(), oldField, newField, details);
		}
	}
}
