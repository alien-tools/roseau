package pkg;

public class FieldsClass {
    // FIELD_NOW_FINAL
    public final int a = 0;

    // FIELD_NOW_STATIC
    public static int b;

    // FIELD_NO_LONGER_STATIC
    public int c;

    // FIELD_TYPE_CHANGED (from String)
    public CharSequence d;

    // FIELD_REMOVED: e is removed in v2

    // FIELD_NOW_PROTECTED
    protected int f;

    // Unchanged field for control
    public int g;
}
