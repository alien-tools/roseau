package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.AnnotationMethodRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class AnnotationMethodNoLongerDefaultRule implements AnnotationMethodRule {
	@Override
	public void onMatchedAnnotationMethod(AnnotationMethodDecl oldMethod, AnnotationMethodDecl newMethod,
	                                      MemberRuleContext ctx) {
		if (oldMethod.hasDefault() && !newMethod.hasDefault()) {
			ctx.builder().memberBC(BreakingChangeKind.ANNOTATION_METHOD_NO_LONGER_DEFAULT, ctx.oldType(), oldMethod, newMethod);
		}
	}
}
