package com.github.maracas.roseau.diff.changes;

/**
 * Enumerates the three possible natures of a breaking change: ADDITION, MUTATION, and DELETION.
 */
public enum BreakingChangeNature {
	/**
	 * Indicates that the breaking change results from an addition to the API.
	 */
	ADDITION,

	/**
	 * Indicates that the breaking change results from changing an existing API symbol.
	 */
	MUTATION,

	/**
	 * Indicates that the breaking change results from a deletion from the API.
	 */
	DELETION
}
