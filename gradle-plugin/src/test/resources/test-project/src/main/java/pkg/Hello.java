package pkg;

/**
 * v2 version of Hello — {@code legacy()} is removed,
 * {@code getName()} returns CharSequence instead of String.
 * Both are breaking changes.
 */
public class Hello {
    public void greet() { System.out.println("hello v2"); }

    // legacy() removed — EXECUTABLE_REMOVED (binary-breaking + source-breaking)

    public CharSequence getName() { return "Hello v2"; }
    // return type changed — METHOD_RETURN_TYPE_ERASURE_CHANGED
}
