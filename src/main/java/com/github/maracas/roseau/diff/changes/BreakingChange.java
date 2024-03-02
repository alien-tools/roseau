package com.github.maracas.roseau.diff.changes;

import com.github.maracas.roseau.api.model.Symbol;

/**
 * A breaking change identified during the comparison of two APIs.
 *
 * @param kind           The kind of the breaking change.
 * @param impactedSymbol The element associated with the breaking change.
 */
public record BreakingChange(
	BreakingChangeKind kind,
	Symbol impactedSymbol
) {
	@Override
	public String toString() {
		return "BC[kind=%s, symbol=%s]".formatted(kind, impactedSymbol.getQualifiedName());
	}
}
