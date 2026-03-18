package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.analysis.TypeParameterMapping;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

/**
 * Detects source-incompatible changes to executable parameter generics after erasure has already been matched.
 * <p>
 * Two distinct source-compatibility checks are needed:
 * <ul>
 *   <li>Overridable executables must preserve overriding relationships under JLS 8.4.2 and JLS 8.4.8.1. Changing a
 *   parameter from a parameterized type to a raw type may therefore still be source-breaking even when call sites are
 *   unaffected, because existing overrides stop being subsignatures.</li>
 *   <li>Final executables and constructors cannot be overridden, so only call-site compatibility matters. Those cases
 *   are checked with {@code isInvocationCompatible(...)} according to JLS 5.3.</li>
 * </ul>
 */
public class ExecutableParameterGenericsChanged implements MemberRule<ExecutableDecl> {
	@Override
	public void onMatched(ExecutableDecl oldExecutable, ExecutableDecl newExecutable, MemberRuleContext ctx) {
		// We matched erasures, so we know parameter types are equals modulo type arguments
		for (int i = 0; i < oldExecutable.getParameters().size(); i++) {
			ParameterDecl p1 = oldExecutable.getParameters().get(i);
			ParameterDecl p2 = newExecutable.getParameters().get(i);

			if (p1.type() instanceof TypeReference<?> pt1 && p2.type() instanceof TypeReference<?> pt2) {
				BreakingChangeDetails details =
					new BreakingChangeDetails.MethodParameterGenericsChanged(pt1, pt2);

				boolean isFinalExecutable = ctx.v1().isEffectivelyFinal(ctx.oldType(), oldExecutable);

				// A raw/parameterized mismatch is always source-breaking for overridable methods because it changes
				// override-equivalence, but final executables must still be checked through invocation compatibility:
				// List<String> -> raw List keeps old call sites valid even though the number of type arguments differs.
				if (!isFinalExecutable && pt1.typeArguments().size() != pt2.typeArguments().size()) {
					ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED,
						ctx.oldType(), oldExecutable, newExecutable, details);
					return;
				}

				TypeParameterMapping.Normalizer normalizer = TypeParameterMapping.Normalizer.forExecutable(
					ctx.oldType(), ctx.newType(), oldExecutable, newExecutable);
				TypeReference<?> normalizedPt1 = (TypeReference<?>) normalizer.normalizeOld(pt1);

				// Overridable executables must preserve override-equivalent signatures. We therefore compare the
				// normalized parameter types directly instead of using invocation compatibility: a change such as
				// List<String> -> raw List keeps old call sites valid but breaks existing overrides.
				if (!isFinalExecutable && !normalizedPt1.equals(pt2)) {
					ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED,
						ctx.oldType(), oldExecutable, newExecutable, details);
				}

				// Final executables and constructors only need to preserve call-site compatibility, so raw/parameterized
				// migration is judged with invocation-context conversions.
				if (isFinalExecutable) {
					TypeReference<?> normalizedPt2 = (TypeReference<?>) normalizer.normalizeNew(pt2);
					if (!ctx.v2().isInvocationCompatible(newExecutable, normalizedPt1, normalizedPt2)) {
						ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_PARAMETER_GENERICS_CHANGED,
							ctx.oldType(), oldExecutable, newExecutable, details);
					}
				}
			}
		}
	}
}
