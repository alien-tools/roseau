package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class AnnotationMethodAddedWithoutDefaultRule implements AnnotationMethodRule {
	@Override
	public void onAddedAnnotationMethod(AnnotationMethodDecl method, MemberRuleContext ctx) {
		if (!method.hasDefault()) {
			ctx.builder().typeBC(BreakingChangeKind.ANNOTATION_METHOD_ADDED_WITHOUT_DEFAULT, ctx.oldType(),
				new BreakingChangeDetails.AnnotationMethodAddedWithoutDefault(method));
		}
	}
}
