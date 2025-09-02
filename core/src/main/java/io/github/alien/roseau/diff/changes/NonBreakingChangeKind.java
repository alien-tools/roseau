package io.github.alien.roseau.diff.changes;

/**
 * Kinds of non-breaking changes detected between two API versions.
 */
public enum NonBreakingChangeKind {
    TYPE_ADDED,
    FIELD_ADDED,
    METHOD_ADDED,
    CONSTRUCTOR_ADDED,

    TYPE_VISIBILITY_INCREASED,
    FIELD_VISIBILITY_INCREASED,
    METHOD_VISIBILITY_INCREASED,
    CONSTRUCTOR_VISIBILITY_INCREASED
}


