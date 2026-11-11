package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.analysis.ApiAnalyzer;
import io.github.alien.roseau.api.analysis.TypeParameterMapping;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class FieldTypeChanged implements MemberRule<FieldDecl> {
	@Override
	public void onMatched(FieldDecl oldField, FieldDecl newField, MemberRuleContext ctx) {
		// Clients link against the erasure of the field's declaration (JLS §13.1), not of the field as seen from the type
		// that supplies type arguments. It may change while the declared type does not (e.g., bound change).
		ApiAnalyzer v1 = ctx.v1().analyzer();
		ApiAnalyzer v2 = ctx.v2().analyzer();
		ITypeReference oldErased = getErasedType(v1, ctx.oldType(), v1.getDeclaration(oldField));
		ITypeReference newErased = getErasedType(v2, ctx.newType(), v2.getDeclaration(newField));
		if (!oldErased.equals(newErased) && !oldField.isCompileTimeConstant() &&
			!isLinkable(oldField, oldErased, ctx)) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED, ctx.oldType(), oldField, newField,
				new BreakingChangeDetails.FieldTypeChanged(oldErased, newErased));
		}

		if (oldField.getType().equals(newField.getType())) {
			return;
		}

		// Normalize type references across versions to handle type parameter renaming (T→U) and generization
		// (Object→T). The forward mapping renames old type params to v2's names; the eraseAdded mapping replaces
		// newly added type params with their erasure (raw-type semantics for existing clients).
		TypeParameterMapping.Normalizer normalizer = TypeParameterMapping.Normalizer.forType(ctx.oldType(), ctx.newType());
		ITypeReference normalizedOld = normalizer.normalizeOld(oldField.getType());
		ITypeReference normalizedNew = normalizer.normalizeNew(newField.getType());

		if (normalizedOld.equals(normalizedNew)) {
			return;
		}

		boolean readCompatible = v2.isSourceCompatibleExpression(ctx.newType(), normalizedOld, normalizedNew);
		boolean writeCompatible = oldField.isFinal() || v2.isAssignable(ctx.newType(), normalizedOld, normalizedNew);

		if (!readCompatible || !writeCompatible) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_TYPE_CHANGED_INCOMPATIBLE, ctx.oldType(), oldField, newField,
				new BreakingChangeDetails.FieldTypeChanged(oldField.getType(), newField.getType()));
		}
	}

	/**
	 * Checks whether a field with the old descriptor remains linkable in the new hierarchy, e.g., a superclass field
	 * hidden by a new field of another type.
	 */
	private static boolean isLinkable(FieldDecl oldField, ITypeReference oldErased, MemberRuleContext ctx) {
		ApiAnalyzer v2 = ctx.v2().analyzer();
		return v2.getLinkableFieldDeclarations(ctx.newType(), oldField.getSimpleName()).stream()
			.anyMatch(decl -> oldErased.equals(getErasedType(v2, ctx.newType(), decl)));
	}

	/**
	 * Returns the erasure of a field declaration's type, in the scope of its declaring type.
	 */
	private static ITypeReference getErasedType(ApiAnalyzer analyzer, TypeDecl owner, FieldDecl declaration) {
		TypeDecl declaringType = analyzer.resolver().resolve(declaration.getContainingType()).orElse(owner);
		return analyzer.getErasedType(declaringType, declaration.getType());
	}
}
