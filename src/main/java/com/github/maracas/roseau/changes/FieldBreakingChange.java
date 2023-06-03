package com.github.maracas.roseau.changes;

import com.github.maracas.roseau.model.FieldDeclaration;

public record FieldBreakingChange(
	FieldDeclaration fieldDeclaration,
	BreakingChangeKind kind
) implements BreakingChange {
}
