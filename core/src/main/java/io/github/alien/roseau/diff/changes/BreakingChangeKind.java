package io.github.alien.roseau.diff.changes;

import static io.github.alien.roseau.diff.changes.BreakingChangeNature.ADDITION;
import static io.github.alien.roseau.diff.changes.BreakingChangeNature.DELETION;
import static io.github.alien.roseau.diff.changes.BreakingChangeNature.MUTATION;

/**
 * Kinds of breaking changes that can be detected when comparing two libraries.
 * <br>
 * These mostly align with the JLS' §13, with additional source-incompatible changes related to generics.
 *
 * <p>
 * Some breaking changes should be refined in the future (e.g., METHOD_RETURN_TYPE_CHANGED is too broad: every return
 * type change is binary-incompatible, only some are source-incompatible). Also, it might make sense at some point to
 * merge some breaking change kinds regardless of the declaration they apply to (e.g., *
 * TYPE/METHOD_FORMAL_TYPE_PARAMETERS_*, *_REMOVED, or *_PROTECTED).
 */
public enum BreakingChangeKind {
	// Type-related BCs
	TYPE_REMOVED(DELETION, true, true),
	TYPE_NOW_PROTECTED(MUTATION, true, true),
	CLASS_NOW_ABSTRACT(MUTATION, true, true),
	CLASS_NOW_FINAL(MUTATION, true, true),
	CLASS_TYPE_CHANGED(MUTATION, true, true),
	CLASS_NOW_CHECKED_EXCEPTION(MUTATION, false, true),
	METHOD_ADDED_TO_INTERFACE(ADDITION, false, true),
	NESTED_CLASS_NOW_STATIC(MUTATION, true, true),
	NESTED_CLASS_NO_LONGER_STATIC(MUTATION, true, true),

	// Field-related BCs
	FIELD_NOW_FINAL(MUTATION, true, true),
	FIELD_NOW_STATIC(MUTATION, true, false),
	FIELD_NO_LONGER_STATIC(MUTATION, true, true),
	FIELD_TYPE_CHANGED(MUTATION, true, true),
	FIELD_REMOVED(DELETION, true, true),
	FIELD_NOW_PROTECTED(MUTATION, true, true),

	// Method-related BCs
	METHOD_REMOVED(DELETION, true, true),
	METHOD_NOW_PROTECTED(MUTATION, true, true),
	METHOD_RETURN_TYPE_CHANGED(MUTATION, true, true),
	METHOD_NOW_ABSTRACT(MUTATION, true, true),
	METHOD_NOW_FINAL(MUTATION, true, true),
	METHOD_NOW_STATIC(MUTATION, true, false),
	METHOD_NO_LONGER_STATIC(MUTATION, true, true),
	METHOD_NOW_THROWS_CHECKED_EXCEPTION(MUTATION, false, true),
	METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION(MUTATION, false, true),
	METHOD_ABSTRACT_ADDED_TO_CLASS(ADDITION, false, true),
	METHOD_PARAMETER_GENERICS_CHANGED(MUTATION, false, true),

	// Constructor-related BCs
	CONSTRUCTOR_REMOVED(DELETION, true, true),
	CONSTRUCTOR_NOW_PROTECTED(MUTATION, true, true),

	// Hierarchy-related BCs
	SUPERTYPE_REMOVED(MUTATION, true, true),

	// Formal type parameters-related BCs
	TYPE_FORMAL_TYPE_PARAMETERS_ADDED(MUTATION, false, true),
	TYPE_FORMAL_TYPE_PARAMETERS_REMOVED(MUTATION, false, true),
	TYPE_FORMAL_TYPE_PARAMETERS_CHANGED(MUTATION, false, true),
	METHOD_FORMAL_TYPE_PARAMETERS_ADDED(MUTATION, false, true),
	METHOD_FORMAL_TYPE_PARAMETERS_REMOVED(MUTATION, false, true),
	METHOD_FORMAL_TYPE_PARAMETERS_CHANGED(MUTATION, false, true),

	// Annotation-related BCs
	ANNOTATION_TARGET_REMOVED(MUTATION, false, true),
	ANNOTATION_METHOD_NO_LONGER_DEFAULT(MUTATION, false, true),
	ANNOTATION_METHOD_ADDED_WITHOUT_DEFAULT(ADDITION, false, true),
	ANNOTATION_NO_LONGER_REPEATABLE(MUTATION, false, true);

	/*
	 * Handled by other BCs:
	 * TYPE_GENERICS_CHANGED
	 * FIELD_GENERICS_CHANGED
	 * METHOD_RETURN_TYPE_GENERICS_CHANGED
	 * METHOD_PARAMETER_GENERICS_CHANGED
	 * METHOD_NO_LONGER_VARARGS
	 * CONSTRUCTOR_PARAMS_GENERICS_CHANGED
	 * CONSTRUCTOR_GENERICS_CHANGED
	 * CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_CHANGED
	 * CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_ADDED
	 * CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_REMOVED
	 *
	 * Compatible changes:
	 * ANNOTATION_DEPRECATED_ADDED
	 * METHOD_NEW_DEFAULT
	 * METHOD_MOVED_TO_SUPERCLASS
	 * METHOD_NOW_VARARGS
	 * METHOD_ABSTRACT_NOW_DEFAULT: ???
	 *
	 * Hierarchy-related that are irrelevant for us:
	 * METHOD_LESS_ACCESSIBLE_THAN_IN_SUPERCLASS
	 * METHOD_IS_STATIC_AND_OVERRIDES_NOT_STATIC
	 * METHOD_IS_NOT_STATIC_AND_OVERRIDES_STATIC
	 * FIELD_STATIC_AND_OVERRIDES_NON_STATIC
	 * FIELD_NON_STATIC_AND_OVERRIDES_STATIC
	 * FIELD_LESS_ACCESSIBLE_THAN_IN_SUPERCLASS
	 */

	private final BreakingChangeNature nature;
	private final boolean binaryBreaking;
	private final boolean sourceBreaking;

	BreakingChangeKind(BreakingChangeNature nature, boolean binaryBreaking, boolean sourceBreaking) {
		this.nature = nature;
		this.binaryBreaking = binaryBreaking;
		this.sourceBreaking = sourceBreaking;
	}

	/**
	 * The breaking change's nature (ADDITION, DELETION, MUTATION)
	 *
	 * @return its nature
	 */
	public BreakingChangeNature getNature() {
		return nature;
	}

	/**
	 * Returns whether this breaking change breaks binary compatibility.
	 *
	 * @return true if this breaking change is binary-breaking
	 */
	public boolean isBinaryBreaking() {
		return binaryBreaking;
	}

	/**
	 * Returns whether this breaking change breaks source compatibility.
	 *
	 * @return true if this breaking change is source-breaking
	 */
	public boolean isSourceBreaking() {
		return sourceBreaking;
	}
}
