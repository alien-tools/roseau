package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

/**
 * A variable arity parameter that becomes an array has the same erasure, but no longer accepts variable arity
 * invocations (JLS §15.12.2.4), e.g., {@code m("a", "b")} for {@code m(String...)} becoming {@code m(String[])}.
 */
public class ExecutableNoLongerVarargs implements MemberRule<ExecutableDecl> {
	@Override
	public void onMatched(ExecutableDecl oldExecutable, ExecutableDecl newExecutable, MemberRuleContext ctx) {
		if (oldExecutable.isVarargs() && !newExecutable.isVarargs()) {
			ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_NO_LONGER_VARARGS, ctx.oldType(), oldExecutable,
				newExecutable);
		}
	}
}
