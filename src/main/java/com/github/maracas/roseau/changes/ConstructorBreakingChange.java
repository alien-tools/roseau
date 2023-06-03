package com.github.maracas.roseau.changes;

import com.github.maracas.roseau.model.ConstructorDeclaration;

public record ConstructorBreakingChange(
	ConstructorDeclaration constructorDeclaration,
	BreakingChangeKind kind
) implements BreakingChange {
}
