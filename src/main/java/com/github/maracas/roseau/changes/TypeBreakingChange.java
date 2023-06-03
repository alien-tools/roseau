package com.github.maracas.roseau.changes;

import com.github.maracas.roseau.model.TypeDeclaration;

public record TypeBreakingChange(
	TypeDeclaration typeDeclaration,
	BreakingChangeKind kind
) implements BreakingChange {
}
