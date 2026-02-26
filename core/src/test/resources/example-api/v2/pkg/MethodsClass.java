package pkg;

import java.util.List;

public class MethodsClass {
    // METHOD_NOW_PROTECTED and METHOD_RETURN_TYPE_ERASURE_CHANGED / METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE
    protected long m1() { return 0L; }

    // METHOD_REMOVED: m2 removed in v2

    // m3 unchanged; we'll use others for static/final
    public final void m3() {}

    // METHOD_NO_LONGER_STATIC
    public void m4() {}

    // METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION and METHOD_NOW_FINAL
    public final void m5() {}

    // METHOD_NOW_STATIC and METHOD_NOW_THROWS_CHECKED_EXCEPTION
    public static void m6() throws java.io.IOException {}

    // METHOD_PARAMETER_GENERICS_CHANGED (List<String> -> List<Number>)
    public void params(List<Number> list) {}
}
