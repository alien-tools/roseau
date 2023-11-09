package com.github.maracas.roseau.changes;

import com.github.maracas.roseau.model.ElementDeclaration;
import com.github.maracas.roseau.model.TypeDeclaration;

/**
 * Represents a breaking change identified during the comparison of APIs between the two library versions.
 * This class encapsulates information about the breaking change's kind, position, nature, and more.
 */
public class BreakingChange {
	/**
	 * The kind of the breaking change.
	 */
	private BreakingChangeKind breakingChangeKind;

	/**
	 * The type in which the breaking change is located.
	 */
	private TypeDeclaration breakingChangeTypeDeclaration;

	/**
	 * The exact position of the breaking change.
	 */
	private String breakingChangePosition;

	/**
	 * The nature of the breaking change ( Addition / deletion / mutation ).
	 */
	private BreakingChangeNature breakingChangeNature;

	/**
	 * The element associated with the breaking change.
	 */
	private ElementDeclaration breakingChangeElement;

	public BreakingChange(BreakingChangeKind breakingChangeKind,
	                      TypeDeclaration breakingChangeTypeDeclaration,
	                      String breakingChangePosition,
	                      BreakingChangeNature breakingChangeNature, ElementDeclaration breakingChangeElement) {
		this.breakingChangeKind = breakingChangeKind;
		this.breakingChangeTypeDeclaration = breakingChangeTypeDeclaration;
		this.breakingChangePosition = breakingChangePosition;
		this.breakingChangeNature = breakingChangeNature;
		this.breakingChangeElement = breakingChangeElement;
	}

	/**
	 * Retrieves the kind of the breaking change.
	 *
	 * @return Breaking change's kind
	 */
	public BreakingChangeKind getBreakingChangeKind() {
		return breakingChangeKind;
	}

	/**
	 * Retrieves the type declaration in which the breaking change is located.
	 *
	 * @return Breaking change's type declaration
	 */
	public TypeDeclaration getBreakingChangeTypeDeclaration() {
		return breakingChangeTypeDeclaration;
	}

	/**
	 * Retrieves the position of the breaking change.
	 *
	 * @return Breaking change's position
	 */
	public String getBreakingChangePosition() {
		return breakingChangePosition;
	}

	/**
	 * Retrieves the nature of the breaking change (Addition / Deletion / Mutation).
	 *
	 * @return Breaking change's nature
	 */
	public BreakingChangeNature getBreakingChangeNature() {
		return breakingChangeNature;
	}

	/**
	 * Retrieves the element associated with the breaking change.
	 *
	 * @return Breaking change's element
	 */
	public ElementDeclaration getBreakingChangeElement() {
		return breakingChangeElement;
	}

	/**
	 * Generates a string representation of the BreakingChange.
	 *
	 * @return A formatted string containing the breaking change's kind, type declaration,
	 * position, nature, and associated element.
	 */
	@Override
	public String toString() {
		return "Breaking Change Kind: " + getBreakingChangeKind() + "\n" +
			"Type Declaration: " + getBreakingChangeTypeDeclaration().getName() + "\n" +
			"Position: " + getBreakingChangePosition() + "\n" +
			"Nature: " + getBreakingChangeNature() + "\n" +
			"Element: " + getBreakingChangeElement().getName() + "\n";
	}
}
