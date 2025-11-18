package io.github.alien.roseau.diff.rules.breaking;

import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.AnnotationRule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class AnnotationNoLongerRepeatableRule implements AnnotationRule {
	@Override
	public void onMatchedAnnotation(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation, TypeRuleContext ctx) {
		if (oldAnnotation.isRepeatable() && !newAnnotation.isRepeatable()) {
			ctx.builder().typeBC(BreakingChangeKind.ANNOTATION_NO_LONGER_REPEATABLE, oldAnnotation);
		}
	}
}
