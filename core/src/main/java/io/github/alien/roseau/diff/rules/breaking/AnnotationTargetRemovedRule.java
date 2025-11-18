package io.github.alien.roseau.diff.rules.breaking;

import com.google.common.collect.Sets;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.Rule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

public class AnnotationTargetRemovedRule implements Rule<AnnotationDecl> {
	@Override
	public void onMatched(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation, TypeRuleContext ctx) {
		Sets.difference(oldAnnotation.getTargets(), newAnnotation.getTargets())
			.forEach(target ->
				ctx.builder().typeBC(BreakingChangeKind.ANNOTATION_TARGET_REMOVED, oldAnnotation,
					new BreakingChangeDetails.AnnotationTargetRemoved(target))
			);
	}
}
