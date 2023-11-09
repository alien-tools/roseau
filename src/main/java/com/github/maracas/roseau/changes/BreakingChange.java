package com.github.maracas.roseau.changes;

import com.github.maracas.roseau.model.ElementDeclaration;
import com.github.maracas.roseau.model.TypeDeclaration;

/**
 * Represents a breaking change identified during the comparison of APIs between the two library versions.
 * This class encapsulates information about the breaking change's kind, position, nature, and more.
 *
 * @param breakingChangeKind            The kind of the breaking change.
 * @param breakingChangeTypeDeclaration The type in which the breaking change is located.
 * @param breakingChangePosition        The exact position of the breaking change.
 * @param breakingChangeNature          The nature of the breaking change ( Addition / deletion / mutation ).
 * @param breakingChangeElement         The element associated with the breaking change.
 */
public record BreakingChange(
	BreakingChangeKind breakingChangeKind,
	TypeDeclaration breakingChangeTypeDeclaration,
	String breakingChangePosition,
	BreakingChangeNature breakingChangeNature,
	ElementDeclaration breakingChangeElement
) {
	/**
	 * Generates a string representation of the BreakingChange.
	 *
	 * @return A formatted string containing the breaking change's kind, type declaration,
	 * position, nature, and associated element.
	 */
	@Override
	public String toString() {
		return "Breaking Change Kind: " + breakingChangeKind() + "\n" +
			"Type Declaration: " + breakingChangeTypeDeclaration().getName() + "\n" +
			"Position: " + breakingChangePosition() + "\n" +
			"Nature: " + breakingChangeNature() + "\n" +
			"Element: " + breakingChangeElement().getName() + "\n";
	}
}
