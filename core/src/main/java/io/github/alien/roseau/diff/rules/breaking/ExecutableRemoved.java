package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class ExecutableRemoved implements MemberRule<ExecutableDecl> {
	@Override
	public void onRemoved(ExecutableDecl executable, MemberRuleContext ctx) {
		if (executable.isMethod()) {
			ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_REMOVED, ctx.oldType(), executable);
		} else if (executable.isConstructor()) {
			ctx.builder().memberBC(BreakingChangeKind.EXECUTABLE_REMOVED, ctx.oldType(), executable);
		}
	}
}
