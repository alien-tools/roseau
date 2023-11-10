package com.github.maracas.roseau.changes;

import com.github.maracas.roseau.model.Symbol;

/**
 * Represents a breaking change identified during the comparison of APIs between the two library versions.
 * This class encapsulates information about the breaking change's kind, position, nature, and more.
 *
 * @param kind            The kind of the breaking change.
 * @param position        The exact position of the breaking change.
 * @param nature          The nature of the breaking change ( Addition / deletion / mutation ).
 * @param impactedSymbol The element associated with the breaking change.
 */
public record BreakingChange(
	BreakingChangeKind kind,
	String position,
	BreakingChangeNature nature,
	Symbol impactedSymbol
) {
	/**
	 * Generates a string representation of the BreakingChange.
	 *
	 * @return A formatted string containing the breaking change's kind, type declaration,
	 * position, nature, and associated element.
	 */
	@Override
	public String toString() {
		return "Breaking Change Kind: " + kind() + "\n" +
			"Position: " + position() + "\n" +
			"Nature: " + nature() + "\n" +
			"Element: " + impactedSymbol().getQualifiedName() + "\n";
	}
}
