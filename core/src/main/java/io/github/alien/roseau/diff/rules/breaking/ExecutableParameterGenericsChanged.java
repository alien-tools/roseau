package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.analysis.TypeParameterMapping;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

import java.util.stream.IntStream;

public class ExecutableParameterGenericsChanged implements MemberRule<ExecutableDecl> {
	@Override
	public void onMatched(ExecutableDecl oldExecutable, ExecutableDecl newExecutable, MemberRuleContext ctx) {
		TypeParameterMapping.Normalizer normalizer = TypeParameterMapping.Normalizer.forExecutable(
			ctx.oldType(), ctx.newType(), oldExecutable, newExecutable);
		boolean isFinalExecutable = ctx.v1().analyzer().isEffectivelyFinal(ctx.oldType(), oldExecutable);
		// §4.8: existing clients see the erasure of the new signature, which accepts the same arguments as the old one
		boolean isErased = ctx.isErasedForExistingClients(newExecutable);
		// §8.4.2: a signature is a subsignature of another if it is its erasure, so overriders and hiders written
		// against an old signature that is the erasure of the new one are unaffected
		boolean isErasureOfNewSignature = !isFinalExecutable && IntStream.range(0, oldExecutable.getParameters().size())
			.allMatch(i -> oldExecutable.getParameters().get(i).type().equals(ctx.v2().analyzer().erasure()
				.getErasedType(newExecutable, newExecutable.getParameters().get(i).type())));

		// We checked executable erasures, so we know parameter types are equals modulo type arguments
		for (int i = 0; i < oldExecutable.getParameters().size(); i++) {
			ParameterDecl p1 = oldExecutable.getParameters().get(i);
			ParameterDecl p2 = newExecutable.getParameters().get(i);

			if (p1.type() instanceof TypeReference<?> pt1 && p2.type() instanceof TypeReference<?> pt2) {
				TypeReference<?> normalizedPt1 = (TypeReference<?>) normalizer.normalizeOld(pt1);
				// §5.1.9: every argument accepted by a raw type is accepted by its unbounded wildcard parameterization
				boolean acceptsRawArguments = pt1.typeArguments().isEmpty() && pt2.typeArguments().stream()
					.allMatch(arg -> arg instanceof WildcardTypeReference wildcard && wildcard.isUnbounded());

				/*
				 * In general, we need to distinguish how formal type parameters and parameter generics are handled between
				 * methods and constructors: the former can be overridden (thus parameters are immutable so that signatures in
				 * sub/super-classes match), and the latter cannot (thus parameters can follow variance rules).
				 */
				boolean compatible;
				if (isFinalExecutable) {
					// Can't be overridden = variance: also erase newly added type params
					compatible = isErased || acceptsRawArguments || ctx.v2().analyzer().isSubtypeOf(newExecutable,
						normalizedPt1, normalizer.normalizeNew(pt2));
				} else if (isErased) {
					compatible = isErasureOfNewSignature;
				} else {
					// Can be overridden = invariant: only rename old type params (do NOT erase new type params, since
					// generizing a type argument like List<Object> → List<T> IS a source break due to name clashes
					// in overriding subclasses)
					compatible = normalizedPt1.equals(pt2) || (acceptsRawArguments && isErasureOfNewSignature);
				}

				if (!compatible) {
					ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED,
						ctx.oldType(), oldExecutable, newExecutable,
						new BreakingChangeDetails.MethodParameterGenericsChanged(pt1, pt2));
					return;
				}
			}
		}
	}
}
