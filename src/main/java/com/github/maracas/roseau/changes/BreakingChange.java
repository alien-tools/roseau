package com.github.maracas.roseau.changes;

import com.github.maracas.roseau.model.Element;
import com.github.maracas.roseau.model.Type;

/**
 * Represents a breaking change identified during the comparison of APIs between the two library versions.
 * This class encapsulates information about the breaking change's kind, position, nature, and more.
 *
 * @param kind            The kind of the breaking change.
 * @param impactedType    The type in which the breaking change is located.
 * @param position        The exact position of the breaking change.
 * @param nature          The nature of the breaking change ( Addition / deletion / mutation ).
 * @param impactedElement The element associated with the breaking change.
 */
public record BreakingChange(
	BreakingChangeKind kind,
	Type impactedType,
	String position,
	BreakingChangeNature nature,
	Element impactedElement
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
			"Type Declaration: " + impactedType().getName() + "\n" +
			"Position: " + position() + "\n" +
			"Nature: " + nature() + "\n" +
			"Element: " + impactedElement().getName() + "\n";
	}
}
