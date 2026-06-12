package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.TypeRule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class ClassNoLongerConcretelyExtensible implements TypeRule<ClassDecl> {
	@Override
	public void onMatched(ClassDecl oldCls, ClassDecl newCls, TypeRuleContext ctx) {
		if (!ctx.v1().analyzer().canHaveConcreteSubtypes(oldCls) || ctx.v2().analyzer().canHaveConcreteSubtypes(newCls)) {
			return;
		}

		// New impossible implementation obligations either appeared (no matching old method) or were already abstract
		// and lost the concrete library override that satisfied them. Report them all.
		for (MethodDecl blocker : ctx.v2().analyzer().getConcreteSubclassBlockers(newCls)) {
			ctx.builder().typeBC(BreakingChangeKind.CLASS_NO_LONGER_CONCRETELY_EXTENSIBLE, oldCls, blocker,
				new BreakingChangeDetails.ClassNoLongerConcretelyExtensible(blocker));
		}
	}
}
