package generated.api.v1;

public record OneRecord(int oneField, char twoField) {
    public static int onePublicStaticField;
    static int oneDefaultStaticField;

    void defaultVoidMethod() {}
    public void publicVoidMethod() {}
    private void privateVoidMethod() {}

    int defaultIntMethod() { return 0; }
    public int publicIntMethod() { return 0; }
    private int privateIntMethod() { return 0; }
}
