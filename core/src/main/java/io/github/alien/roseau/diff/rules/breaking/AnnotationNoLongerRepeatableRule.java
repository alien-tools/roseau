package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.Rule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class AnnotationNoLongerRepeatableRule implements Rule<AnnotationDecl> {
	@Override
	public void onMatched(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation, TypeRuleContext ctx) {
		if (oldAnnotation.isRepeatable() && !newAnnotation.isRepeatable()) {
			ctx.builder().typeBC(BreakingChangeKind.ANNOTATION_NO_LONGER_REPEATABLE, oldAnnotation);
		}
	}
}
