package pkg;

public class MethodTypeParameters {
    // METHOD_FORMAL_TYPE_PARAMETERS_REMOVED in v2
    public <T> void removedTP(String s) {}

    // METHOD_FORMAL_TYPE_PARAMETERS_ADDED in v2
    public void addedTP(String s) {}

    // METHOD_FORMAL_TYPE_PARAMETERS_CHANGED in v2
    public <T extends Number> void changedTP(String s) {}
}
