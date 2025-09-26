package io.github.alien.roseau.diff.changes;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.Symbol;

/**
 * A breaking change identified when comparing two {@link LibraryTypes} instances.
 *
 * @param kind           The kind of breaking change
 * @param impactedSymbol The API symbol impacted by the breaking change
 * @param newSymbol      If applicable, the corresponding symbol in the new version
 * @param details        Additional details about the breaking change
 * @see BreakingChangeKind
 */
public record BreakingChange(
	BreakingChangeKind kind,
	Symbol impactedSymbol,
	Symbol newSymbol,
	BreakingChangeDetails details
) {
	public BreakingChange {
		Preconditions.checkNotNull(kind);
		Preconditions.checkNotNull(impactedSymbol);
		if (details == null) {
			details = new BreakingChangeDetails.None();
		}
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
