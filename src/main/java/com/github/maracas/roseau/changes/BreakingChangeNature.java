package com.github.maracas.roseau.changes;

/**
 * Enumerates the three possible natures of a breaking change: ADDITION, MUTATION, and DELETION.
 */
public enum BreakingChangeNature {
    /**
     * Indicates that the breaking change is a result of an addition to the API.
     */
    ADDITION,

    /**
     * Indicates that the breaking change results from an alteration of existing elements within the API.
     */
    MUTATION,

    /**
     *  Indicates that the breaking change is a result of a deletion from the API.
     */
    DELETION
}
