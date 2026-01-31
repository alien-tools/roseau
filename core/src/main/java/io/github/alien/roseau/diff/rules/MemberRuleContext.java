package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.RoseauReport;

public record MemberRuleContext(
	API v1,
	API v2,
	TypeDecl oldType,
	TypeDecl newType,
	RoseauReport.Builder builder
) {
}
