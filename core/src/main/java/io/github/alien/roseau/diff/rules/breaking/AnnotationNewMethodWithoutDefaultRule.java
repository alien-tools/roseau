package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class AnnotationNewMethodWithoutDefaultRule implements MemberRule<AnnotationMethodDecl> {
	@Override
	public void onAdded(AnnotationMethodDecl method, MemberRuleContext ctx) {
		if (!method.hasDefault()) {
			ctx.builder().typeBC(BreakingChangeKind.ANNOTATION_NEW_METHOD_WITHOUT_DEFAULT, ctx.oldType(),
				new BreakingChangeDetails.AnnotationNewMethodWithoutDefault(method));
		}
	}
}
