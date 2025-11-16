package io.github.alien.roseau.diff.rules;

import com.google.common.collect.Sets;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

public class AnnotationTargetRemovedRule implements AnnotationRule {
	@Override
	public void onMatchedAnnotation(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation,TypeRuleContext ctx) {
		Sets.difference(oldAnnotation.getTargets(), newAnnotation.getTargets())
			.forEach(target ->
				ctx.builder().typeBC(BreakingChangeKind.ANNOTATION_TARGET_REMOVED, oldAnnotation,
					new BreakingChangeDetails.AnnotationTargetRemoved(target))
			);
	}
}
