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
	TYPE_REMOVED(DELETION),
	CLASS_NOW_ABSTRACT(MUTATION),
	CLASS_NOW_FINAL(MUTATION),
	NESTED_CLASS_NOW_STATIC(MUTATION),
	NESTED_CLASS_NO_LONGER_STATIC(MUTATION),
	CLASS_TYPE_CHANGED(MUTATION),
	CLASS_NOW_CHECKED_EXCEPTION(MUTATION),
	TYPE_NOW_PROTECTED(MUTATION),
	SUPERTYPE_REMOVED(MUTATION),
	TYPE_FORMAL_TYPE_PARAMETERS_ADDED(MUTATION),
	TYPE_FORMAL_TYPE_PARAMETERS_REMOVED(MUTATION),
	TYPE_FORMAL_TYPE_PARAMETERS_CHANGED(MUTATION),
	/**
	 * A breaking change where a method has been removed or its accessibility has
	 * been reduced.
	 * <p>
	 * This occurs when a method that was present in the old version of a library is
	 * either:
	 * <p>
	 * - No longer present in the new version.
	 * <p>
	 * - Its visibility has been reduced, making it inaccessible from some or all of
	 * the previously allowed access points.
	 * <p>
	 * - It has been overloaded or its parameters have been changed, effectively
	 * removing the original method.
	 * <p>
	 * - A class or interface no longer extends or implements the parent class or
	 * interface that contains the method.
	 *
	 * <p>
	 * In the following sections, you will find a list of examples illustrating when this breaking change can occur.
	 * 
	 * <p>1. A method is completely removed in the new version :
	 *
	 * {@snippet :
	 * // Version 1
	 * public class A {
	 *	 public void m1() {}
	 * }
	 *
	 * // Version 2
	 * public class A {}
	 * }
	 * 
	 * <p>2. The visibility of a method is reduced, making it inaccessible from some or all previously allowed access points :
	 *
	 * {@snippet :
	 * // Version 1
	 * public class A {
	 *   public void m1() {}
	 * }
	 *
	 * // Version 2
	 * public class A {
	 *   void m1() {}
	 * }
	 * }
	 *
	 *<p>3. An overloaded method is removed in the new version :
	 *
	 * {@snippet :
	 * // Version 1
	 * public class A {
	 *   public void m1() {}
	 * 
	 *   public void m1(int i) {}
	 * }
	 *
	 * // Version 2
	 * public class A {
	 *   public void m1() {}
	 * }
	 * }
	 *
	 * <p>4. A static method is removed in the new version :
	 *
	 * {@snippet :
	 * // Version 1
	 * public class A {
	 *   public static void m1() {}
	 * }
	 *
	 * // Version 2
	 * public class A {}
	 * }
	 *
	 * <p>5. A default method is removed from an interface in the new version :
	 *
	 * {@snippet :
	 * // Version 1
	 * public interface I {
	 *	 default void m1() {}
	 * }
	 *
	 * // Version 2
	 * public interface I {}
	 * }
	 *
	 * <p>6. A method's visibility is reduced from public to package-private :
	 *
	 * {@snippet :
	 * // Version 1
	 * public class A {
	 *   public void m1() {}
	 * }
	 *
	 * // Version 2
	 * public class A {
	 *    void m1() {}
	 * }
	 * }
	 * 
	 *<p> 7. A subbclass no longer overrided a method from its superclass :
	 *
	 * {@snippet :
	 * // Version 1
	 * public class A {
	 *   public void m1() {}
	 * }
	 * public class B extends A {
	 *	 public void m1() {}
	 * }
	 * 
	 * // Version 2
	 * public class A {
	 *	 public void m1() {}
	 * }
	 * public class B extends A {}
	 * }
	 * 
	 * <p> 8. A method is removed from an interface, affecting classes that implement this interface :
	 *
	 * {@snippet :
	 * // Version 1
	 * public interface I {
	 *   void m1();
	 * }
	 * public class A implements I {
	 *  public void m1() {}
	 * } 
	 * 
	 * // Version 2
	 * public interface I {}
	 * public class A implements I {}
	 * }
	 * 
	 * <p> 9. A method no longer extends parent class that contains the method :
	 * 	
	 * {@snippet :
	 * // Version 1
	 * class A {
	 *	public void m1() {}
	 *	public class B extends A {
	 *	  public void m2() {}
	 *	}
	 *	
	 *	// Version 2
	 *  class A {
	 *	  public void m1() {}
	 *	}
	 *	public class B {
	 *	  public void m2() {}
	 *	}
	 * }
	 * }
	 * 
	 * <p> 10. The arguments of a method are changed :
	 * 
	 * {@snippet :
	 * // Version 1
	 * public class A {
	 *  public void m1(int x, String y) {}
	 * }
	 * 
	 * // Version 2
	 * public class A {
	 *  public void m1(int x) {}
	 * }
	 * }
	 */
	METHOD_REMOVED(DELETION),
	METHOD_NOW_PROTECTED(MUTATION),
	METHOD_RETURN_TYPE_CHANGED(MUTATION),
	METHOD_NOW_ABSTRACT(MUTATION),
	METHOD_NOW_FINAL(MUTATION),
	METHOD_NOW_STATIC(MUTATION),
	METHOD_NO_LONGER_STATIC(MUTATION),
	METHOD_NO_LONGER_VARARGS(MUTATION),
	METHOD_NOW_THROWS_CHECKED_EXCEPTION(MUTATION),
	METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION(MUTATION),
	METHOD_ABSTRACT_ADDED_TO_CLASS(ADDITION),
	METHOD_ADDED_TO_INTERFACE(ADDITION),
	METHOD_FORMAL_TYPE_PARAMETERS_ADDED(MUTATION),
	METHOD_FORMAL_TYPE_PARAMETERS_REMOVED(MUTATION),
	METHOD_FORMAL_TYPE_PARAMETERS_CHANGED(MUTATION),
	METHOD_PARAMETER_GENERICS_CHANGED(MUTATION),

	FIELD_NOW_FINAL(MUTATION),
	FIELD_NOW_STATIC(MUTATION),
	FIELD_NO_LONGER_STATIC(MUTATION),
	FIELD_TYPE_CHANGED(MUTATION),
	FIELD_REMOVED(DELETION),
	FIELD_NOW_PROTECTED(MUTATION),

	CONSTRUCTOR_REMOVED(DELETION),
	CONSTRUCTOR_NOW_PROTECTED(MUTATION);

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

	BreakingChangeKind(BreakingChangeNature nature) {
		this.nature = nature;
	}

	public BreakingChangeNature getNature() {
		return nature;
	}
}
