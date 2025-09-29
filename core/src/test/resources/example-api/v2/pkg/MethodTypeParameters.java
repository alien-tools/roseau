package pkg;

public class MethodTypeParameters {
    // METHOD_FORMAL_TYPE_PARAMETERS_REMOVED
    public void removedTP(String s) {}

    // METHOD_FORMAL_TYPE_PARAMETERS_ADDED
    public <U> void addedTP(String s) {}

    // METHOD_FORMAL_TYPE_PARAMETERS_CHANGED (bounds change)
    public <T extends CharSequence> void changedTP(String s) {}
}
