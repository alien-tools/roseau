package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.ExecutableRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class ExecutableRemovedRule implements ExecutableRule {
	@Override
	public void onRemovedExecutable(ExecutableDecl executable, MemberRuleContext ctx) {
		if (executable.isMethod()) {
			ctx.builder().memberBC(BreakingChangeKind.METHOD_REMOVED, ctx.oldType(), executable);
		} else if (executable.isConstructor()) {
			ctx.builder().memberBC(BreakingChangeKind.CONSTRUCTOR_REMOVED, ctx.oldType(), executable);
		}
	}
}
