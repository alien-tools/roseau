package io.github.alien.roseau.diff.changes;

import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.Symbol;

import java.util.Objects;

/**
 * A breaking change identified when comparing two {@link LibraryTypes} instances.
 *
 * @param kind           The kind of breaking change
 * @param impactedSymbol The API symbol impacted by the breaking change
 * @param newSymbol      If applicable, the corresponding symbol in the new version
 * @see BreakingChangeKind
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

	private static String printSymbol(Symbol s) {
		if (s == null) {
			return "";
		} else if (s instanceof ExecutableDecl e) {
			return String.format("%s.%s", e.getContainingType().getQualifiedName(), e.getSignature());
		} else {
			return s.getQualifiedName();
		}
	}

	@Override
	public String toString() {
		return "BC[kind=%s, impactedSymbol=%s, newSymbol=%s]".formatted(kind,
			printSymbol(impactedSymbol), printSymbol(newSymbol));
	}
}
