package com.github.maracas.roseau.changes;

import com.github.maracas.roseau.model.MethodDeclaration;

public record MethodBreakingChange(
	MethodDeclaration methodDeclaration,
	BreakingChangeKind kind
) implements BreakingChange {
}
