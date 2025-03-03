package com.github.maracas.roseau.diff.changes;

import static com.github.maracas.roseau.diff.changes.BreakingChangeNature.ADDITION;
import static com.github.maracas.roseau.diff.changes.BreakingChangeNature.DELETION;
import static com.github.maracas.roseau.diff.changes.BreakingChangeNature.MUTATION;

/*
 * Kinds of breaking changes that can be detected.
 *
 * FIXME: we're currently mostly focused on binary compatibility without
 * much thought given to source compatibility except for generics.
 * We'll probably have to map BCs to their impact on binary/source compatibility
 * and refine some (e.g., METHOD_RETURN_TYPE_CHANGED is too broad: every return
 * type change is binary-incompatible, only some are source-incompatible).
 *
 * It might make sense at some point to merge BC kinds regardless of the declaration
 * they apply to (e.g., TYPE/METHOD_FORMAL_TYPE_PARAMETERS_*, *_REMOVED, or *_PROTECTED.
 */
public enum BreakingChangeKind {
	TYPE_REMOVED(DELETION, true, true),
	CLASS_NOW_ABSTRACT(MUTATION, true, true),
	CLASS_NOW_FINAL(MUTATION, true, true),
	NESTED_CLASS_NOW_STATIC(MUTATION, true, true),
	NESTED_CLASS_NO_LONGER_STATIC(MUTATION, true, true),
	CLASS_TYPE_CHANGED(MUTATION, true, true),
	CLASS_NOW_CHECKED_EXCEPTION(MUTATION, false, true),
	TYPE_NOW_PROTECTED(MUTATION, true, true),
	SUPERTYPE_REMOVED(MUTATION, true, true),
	TYPE_FORMAL_TYPE_PARAMETERS_ADDED(MUTATION, false, true),
	TYPE_FORMAL_TYPE_PARAMETERS_REMOVED(MUTATION, false, true),
	TYPE_FORMAL_TYPE_PARAMETERS_CHANGED(MUTATION, false, true),
	/**
	 * A breaking change where a method has been removed or its accessibility has
	 * been reduced.
	 * <p>
	 * This occurs when a method that was present in the old version of a library is
	 * either:
	 * <ul>
	 * <li>No longer present in the new version.
	 * <li>Its visibility has been reduced, making it inaccessible from some or all of
	 * the previously allowed access points.
	 * <li>It has been overloaded or its parameters have been changed, effectively
	 * removing the original method.
	 * <li> A class or interface no longer extends or implements the parent class or
	 * interface that contains the method.
	 * </ul>
	 */
	METHOD_REMOVED(DELETION, true, true),
	METHOD_NOW_PROTECTED(MUTATION, true, true),
	METHOD_RETURN_TYPE_CHANGED(MUTATION, true, true),
	METHOD_NOW_ABSTRACT(MUTATION, true, true),
	METHOD_NOW_FINAL(MUTATION, true, true),
	METHOD_NOW_STATIC(MUTATION, true, false),
	METHOD_NO_LONGER_STATIC(MUTATION, true, true),
	METHOD_NO_LONGER_VARARGS(MUTATION, false, true),
	METHOD_NOW_THROWS_CHECKED_EXCEPTION(MUTATION, false, true),
	METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION(MUTATION, false, true),
	METHOD_ABSTRACT_ADDED_TO_CLASS(ADDITION, false, true),
	METHOD_ADDED_TO_INTERFACE(ADDITION, false, true),
	METHOD_FORMAL_TYPE_PARAMETERS_ADDED(MUTATION, false, true),
	METHOD_FORMAL_TYPE_PARAMETERS_REMOVED(MUTATION, false, true),
	METHOD_FORMAL_TYPE_PARAMETERS_CHANGED(MUTATION, false, true),
	METHOD_PARAMETER_GENERICS_CHANGED(MUTATION, false, true),

	FIELD_NOW_FINAL(MUTATION, true, true),
	FIELD_NOW_STATIC(MUTATION, true, false),
	FIELD_NO_LONGER_STATIC(MUTATION, true, true),
	FIELD_TYPE_CHANGED(MUTATION, true, true),
	FIELD_REMOVED(DELETION, true, true),
	FIELD_NOW_PROTECTED(MUTATION, true, true),

	CONSTRUCTOR_REMOVED(DELETION, true, true),
	CONSTRUCTOR_NOW_PROTECTED(MUTATION, true, true);

	/*
	 * To implement, maybe
	 * TYPE_GENERICS_CHANGED
	 * 
	 * FIELD_GENERICS_CHANGED
	 * 
	 * METHOD_RETURN_TYPE_GENERICS_CHANGED
	 * METHOD_PARAMETER_GENERICS_CHANGED
	 * 
	 * CONSTRUCTOR_PARAMS_GENERICS_CHANGED
	 * CONSTRUCTOR_GENERICS_CHANGED
	 * CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_CHANGED
	 * CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_ADDED
	 * CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_REMOVED
	 */

	/*
	 * Compatible changes
	 * ANNOTATION_DEPRECATED_ADDED
	 * METHOD_NEW_DEFAULT
	 * METHOD_MOVED_TO_SUPERCLASS
	 * METHOD_NOW_VARARGS
	 * METHOD_ABSTRACT_NOW_DEFAULT: ???
	 */

	/*
	 * Hierarchy-related that are irrelevant for us?
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

	public BreakingChangeNature getNature() {
		return nature;
	}

	public boolean isBinaryBreaking() {
		return binaryBreaking;
	}

	public boolean isSourceBreaking() {
		return sourceBreaking;
	}
}
