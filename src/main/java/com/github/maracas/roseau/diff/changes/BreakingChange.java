package com.github.maracas.roseau.diff.changes;

import com.github.maracas.roseau.api.model.ExecutableDecl;
import com.github.maracas.roseau.api.model.Symbol;

import java.util.Objects;

/**
 * A breaking change identified during the comparison of two APIs.
 *
 * @param kind           The kind of the breaking change.
 * @param impactedSymbol The element associated with the breaking change.
 */
public record BreakingChange(
	BreakingChangeKind kind,
	Symbol impactedSymbol,
	Symbol newSymbol
) {
	public BreakingChange {
		Objects.requireNonNull(kind);
		Objects.requireNonNull(impactedSymbol);
	}

	public String printSymbol(Symbol s) {
		if (s == null)
			return "";
		if (s instanceof ExecutableDecl e)
			return String.format("%s.%s", e.getContainingType().getQualifiedName(), e.getSignature());
		return s.getQualifiedName();
	}

	@Override
	public String toString() {
		return "BC[kind=%s, impactedSymbol=%s, newSymbol=%s]".formatted(kind,
			printSymbol(impactedSymbol), printSymbol(newSymbol));
	}
}
