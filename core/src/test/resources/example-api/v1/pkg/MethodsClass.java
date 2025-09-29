package pkg;

import java.util.List;

public class MethodsClass {
    // METHOD_NOW_PROTECTED and METHOD_RETURN_TYPE_CHANGED in v2
    public int m1() { return 0; }

    // METHOD_REMOVED in v2
    public void m2() {}

    // METHOD_NOW_STATIC in v2 (we'll make this one static later) — actually we'll convert m6
    public final void m3() {}

    // METHOD_NO_LONGER_STATIC in v2
    public static void m4() {}

    // METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION in v2
    public void m5() throws java.io.IOException {}

    // METHOD_NOW_STATIC in v2 and METHOD_NOW_THROWS_CHECKED_EXCEPTION in v2
    public void m6() {}

    // METHOD_PARAMETER_GENERICS_CHANGED in v2 (List<String> -> List<Number>)
    public void params(List<String> list) {}
}
