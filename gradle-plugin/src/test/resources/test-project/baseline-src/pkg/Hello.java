package pkg;

/**
 * Baseline (v1) version of Hello — has a {@code legacy()} method
 * that will be removed in v2, and {@code getName()} returning String.
 */
public class Hello {
    public void greet() { System.out.println("hello v1"); }

    /** @deprecated removed in v2 */
    public void legacy() { }

    public String getName() { return "Hello"; }
}
