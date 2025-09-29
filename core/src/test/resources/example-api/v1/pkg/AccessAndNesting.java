package pkg;

public class AccessAndNesting {
    // TYPE_NOW_PROTECTED (via nested type): will become protected in v2
    public static class PublicNested {
    }

    // NESTED_CLASS_NOW_STATIC (v2 changes InnerA to static)
    public class InnerA {
    }

    // NESTED_CLASS_NO_LONGER_STATIC (v2 removes static)
    public static class InnerB {
    }
}
