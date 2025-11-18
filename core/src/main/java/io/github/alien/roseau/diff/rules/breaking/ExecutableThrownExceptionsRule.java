package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.ExecutableRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

import java.util.Set;

public class ExecutableThrownExceptionsRule implements ExecutableRule {
	/**
	 * Always binary-compatible.
	 * <ul>
	 *   <li>Throwing a new checked exception breaks invokers only</li>
	 *   <li>No longer throwing a checked exception breaks invokers and overriders</li>
	 *   <li>Throwing a subtype of an existing checked exception (less) breaks overriders</li>
	 *   <li>Throwing a supertype of an existing checked exception (more) breaks invokers</li>
	 *   <li>The only safe case is replacing with a subtype exception when the executable is final</li>
	 * </ul>
	 */
	@Override
	public void onMatchedExecutable(ExecutableDecl oldExecutable, ExecutableDecl newExecutable, MemberRuleContext ctx) {
		Set<ITypeReference> thrown1 = ctx.v1().getThrownCheckedExceptions(oldExecutable);
		Set<ITypeReference> thrown2 = ctx.v2().getThrownCheckedExceptions(newExecutable);

		thrown1.stream()
			.filter(exc1 -> thrown2.stream().noneMatch(exc2 ->
				// FIXME: correct, but meh
				ctx.v2().isSubtypeOf(exc1, exc2) || ctx.v1().isEffectivelyFinal(ctx.oldType(), oldExecutable)
			))
			.forEach(exc1 ->
				ctx.builder().memberBC(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, ctx.oldType(), oldExecutable, newExecutable,
					new BreakingChangeDetails.MethodNoLongerThrowsCheckedException(exc1)));

		thrown2.stream()
			.filter(exc2 -> thrown1.stream().noneMatch(exc1 -> ctx.v2().isSubtypeOf(exc2, exc1)))
			.forEach(exc2 ->
				ctx.builder().memberBC(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, ctx.oldType(), oldExecutable, newExecutable,
					new BreakingChangeDetails.MethodNowThrowsCheckedException(exc2)));
	}
}
