package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.TypeParameterScope;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

import java.util.Set;

public class ExecutableThrownExceptions implements MemberRule<ExecutableDecl> {
	/**
	 * Changes to a throws clause are always binary-compatible (JLS §13.4.21). Only checked exceptions matter.
	 * <ul>
	 *   <li>A newly thrown exception that is not a subtype of a previously thrown one breaks invokers that do not
	 *   handle it (JLS §11.2.3)</li>
	 *   <li>A previously thrown exception that is not a subtype of a newly thrown one breaks invokers whose catch
	 *   clauses name it or one of its subclasses (JLS §11.2.3), and overriders that declare it (JLS §8.4.8.3).
	 *   Invokers break even when the executable cannot be overridden: replacing {@code IOException} with its subtype
	 *   {@code ObjectStreamException} breaks {@code catch (FileNotFoundException e)}.</li>
	 * </ul>
	 */
	@Override
	public void onMatched(ExecutableDecl oldExecutable, ExecutableDecl newExecutable, MemberRuleContext ctx) {
		Set<ITypeReference> thrown1 = ctx.v1().analyzer().getThrownCheckedExceptions(oldExecutable);
		Set<ITypeReference> thrown2 = ctx.v2().analyzer().getThrownCheckedExceptions(newExecutable);

		thrown1.stream()
			// An exception that is no longer checked in v2 constrains no client: callers need not catch it and
			// overriders need not declare it, so removing (or keeping) it from the throws clause is compatible.
			.filter(exc1 -> isChecked(exc1, ctx))
			.filter(exc1 -> thrown2.stream()
				.noneMatch(exc2 -> ctx.v2().analyzer().isSubtypeOf(TypeParameterScope.EMPTY, exc1, exc2)))
			.forEach(exc1 ->
				ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_NO_LONGER_THROWS_CHECKED_EXCEPTION,
					ctx.oldType(), oldExecutable, newExecutable,
					new BreakingChangeDetails.MethodNoLongerThrowsCheckedException(exc1)));

		thrown2.stream()
			.filter(exc2 -> thrown1.stream()
				.noneMatch(exc1 -> ctx.v2().analyzer().isSubtypeOf(TypeParameterScope.EMPTY, exc2, exc1)))
			.forEach(exc2 ->
				ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_NOW_THROWS_CHECKED_EXCEPTION,
					ctx.oldType(), oldExecutable, newExecutable,
					new BreakingChangeDetails.MethodNowThrowsCheckedException(exc2)));
	}

	private static boolean isChecked(ITypeReference exception, MemberRuleContext ctx) {
		boolean goneInV2 = exception instanceof TypeReference<?> reference &&
			ctx.v2().analyzer().resolver().resolve(reference).isEmpty();
		API version = goneInV2 ? ctx.v1() : ctx.v2();
		return version.analyzer().isCheckedException(exception);
	}
}
