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
	TYPE_KIND_CHANGED(MUTATION, true, true),
	TYPE_SUPERTYPE_REMOVED(MUTATION, true, true),
	TYPE_NEW_ABSTRACT_METHOD(ADDITION, false, true),

	// Class-related BCs,
	CLASS_NOW_ABSTRACT(MUTATION, true, true),
	CLASS_NOW_FINAL(MUTATION, true, true),
	CLASS_NOW_CHECKED_EXCEPTION(MUTATION, false, true),
	CLASS_NOW_STATIC(MUTATION, true, true),
	CLASS_NO_LONGER_STATIC(MUTATION, true, true),

	// Annotation-related BCs
	ANNOTATION_TARGET_REMOVED(MUTATION, false, true),
	ANNOTATION_NEW_METHOD_WITHOUT_DEFAULT(ADDITION, false, true),
	ANNOTATION_NO_LONGER_REPEATABLE(MUTATION, false, true),
	ANNOTATION_METHOD_NO_LONGER_DEFAULT(MUTATION, false, true),

	// Field-related BCs
	FIELD_REMOVED(DELETION, true, true),
	FIELD_NOW_PROTECTED(MUTATION, true, true),
	FIELD_TYPE_CHANGED(MUTATION, true, true),
	FIELD_NOW_FINAL(MUTATION, true, true),
	FIELD_NOW_STATIC(MUTATION, true, false),
	FIELD_NO_LONGER_STATIC(MUTATION, true, true),

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
	METHOD_PARAMETER_GENERICS_CHANGED(MUTATION, false, true),

	// Constructor-related BCs
	CONSTRUCTOR_REMOVED(DELETION, true, true),
	CONSTRUCTOR_NOW_PROTECTED(MUTATION, true, true),

	// Formal type parameters-related BCs
	FORMAL_TYPE_PARAMETER_ADDED(MUTATION, false, true),
	FORMAL_TYPE_PARAMETER_REMOVED(MUTATION, false, true),
	FORMAL_TYPE_PARAMETER_CHANGED(MUTATION, false, true);

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
