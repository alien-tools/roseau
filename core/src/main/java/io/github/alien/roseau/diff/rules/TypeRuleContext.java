package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.RoseauReport;

public record TypeRuleContext(
	API v1,
	API v2,
	RoseauReport.Builder builder
) {
}
