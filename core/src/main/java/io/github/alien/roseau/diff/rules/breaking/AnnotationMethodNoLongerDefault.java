package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;

public class AnnotationMethodNoLongerDefault implements MemberRule<AnnotationMethodDecl> {
	@Override
	public void onMatched(AnnotationMethodDecl oldMethod, AnnotationMethodDecl newMethod,
	                                      MemberRuleContext ctx) {
		if (oldMethod.hasDefault() && !newMethod.hasDefault()) {
			ctx.builder().memberBC(BreakingChangeKind.ANNOTATION_METHOD_NO_LONGER_DEFAULT, ctx.oldType(), oldMethod, newMethod);
		}
	}
}
