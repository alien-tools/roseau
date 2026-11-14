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
		// Clients link against the erasure of the field's declaration (JLS §13.1), not of the field as seen from the
		// type that supplies type arguments. It may change while the declared type does not (e.g., bound change).
		ApiAnalyzer v1 = ctx.v1().analyzer();
		ApiAnalyzer v2 = ctx.v2().analyzer();
		ITypeReference oldErased = getErasedType(v1, ctx.oldType(), v1.getDeclaration(oldField));
		ITypeReference newErased = getErasedType(v2, ctx.newType(), v2.getDeclaration(newField));
		if (!oldErased.equals(newErased) && !oldField.isCompileTimeConstant() &&
			!isLinkable(oldField, oldErased, ctx)) {
			ctx.builder().memberBC(BreakingChangeKind.FIELD_TYPE_ERASURE_CHANGED, ctx.oldType(), oldField, newField,
				new BreakingChangeDetails.FieldTypeChanged(oldErased, newErased));
		}

		// Clients that parameterize the type see the field as it is declared, and those that use the type raw see its
		// erasure (§4.8). The existing clients of a type that becomes generic can only use it raw.
		TypeParameterMapping.Normalizer normalizer = TypeParameterMapping.Normalizer.forType(ctx.oldType(), ctx.newType());
		ITypeReference normalizedOld = normalizer.normalizeOld(oldField.getType());
		if (!ctx.isErasedForExistingClients(newField) &&
			!isCompatible(oldField, normalizedOld, normalizer.normalizeNew(newField.getType()), ctx)) {
			report(oldField, newField, oldField.getType(), newField.getType(), ctx);
		} else if (v2.isErasedInRawType(ctx.newType(), newField)) {
			boolean wasErased = v1.isErasedInRawType(ctx.oldType(), oldField);
			if (!isCompatible(oldField, wasErased ? oldErased : normalizedOld, newErased, ctx)) {
				report(oldField, newField, wasErased ? oldErased : oldField.getType(), newErased, ctx);
			}
		}
	}

	private static boolean isCompatible(FieldDecl oldField, ITypeReference oldType, ITypeReference newType,
	                                    MemberRuleContext ctx) {
		if (oldType.equals(newType)) {
			return true;
		}

		ApiAnalyzer v2 = ctx.v2().analyzer();
		boolean readCompatible = v2.isSourceCompatibleExpression(ctx.newType(), oldType, newType);
		boolean writeCompatible = oldField.isFinal() || v2.isSourceCompatibleVariable(ctx.newType(), oldType, newType);
		return readCompatible && writeCompatible;
	}

	private static void report(FieldDecl oldField, FieldDecl newField, ITypeReference oldType, ITypeReference newType,
	                           MemberRuleContext ctx) {
		ctx.builder().memberBC(BreakingChangeKind.FIELD_TYPE_CHANGED_INCOMPATIBLE, ctx.oldType(), oldField, newField,
			new BreakingChangeDetails.FieldTypeChanged(oldType, newType));
	}

	/**
	 * Checks whether a field with the old descriptor remains linkable in the new hierarchy, e.g., a superclass field
	 * hidden by a new field of another type. Compiled accesses also expect it to be as accessible as the old field,
	 * and static if it was.
	 */
	private static boolean isLinkable(FieldDecl oldField, ITypeReference oldErased, MemberRuleContext ctx) {
		ApiAnalyzer v2 = ctx.v2().analyzer();
		return v2.getLinkableFieldDeclarations(ctx.newType(), oldField.getSimpleName()).stream()
			.filter(decl -> decl.isStatic() == oldField.isStatic() && (decl.isPublic() || !oldField.isPublic()))
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
