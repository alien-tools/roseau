package pkg;

public class FieldsClass {
    // FIELD_NOW_FINAL in v2
    public int a;

    // FIELD_NOW_STATIC in v2
    public int b;

    // FIELD_NO_LONGER_STATIC in v2
    public static int c;

    // FIELD_TYPE_CHANGED in v2 (to CharSequence)
    public String d;

    // FIELD_REMOVED in v2
    public int e;

    // FIELD_NOW_PROTECTED in v2
    public int f;

    // Unchanged field for control
    public int g;
}
