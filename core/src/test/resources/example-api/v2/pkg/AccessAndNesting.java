package pkg;

public class AccessAndNesting {
    // TYPE_NOW_PROTECTED: nested type made protected
    protected static class PublicNested {
    }

    // NESTED_CLASS_NOW_STATIC
    public static class InnerA {
    }

    // NESTED_CLASS_NO_LONGER_STATIC
    public class InnerB {
    }
}
