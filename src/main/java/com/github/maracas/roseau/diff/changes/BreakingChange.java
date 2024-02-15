package com.github.maracas.roseau.diff.changes;

import com.github.maracas.roseau.api.model.Symbol;

import java.nio.file.Path;

import static com.diogonunes.jcolor.Ansi.colorize;
import static com.diogonunes.jcolor.Attribute.*;

/**
 * Represents a breaking change identified during the comparison of APIs between the two library versions.
 * This class encapsulates information about the breaking change's kind and impacted symbol.
 *
 * @param kind            The kind of the breaking change.
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

	public String format() {
		return String.format("%s %s",
				colorize(kind.toString(), RED_TEXT(), BOLD()),
				colorize(impactedSymbol.getQualifiedName(), UNDERLINE()));
	}
}
