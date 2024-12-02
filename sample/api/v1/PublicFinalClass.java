package sample.api.v1;

public final class PublicFinalClass {
    public int publicInt = 0;
    private char privateChar = 'c';

    public PublicFinalClass() {}

    public void publicVoidMethod() {}
    private void privateVoidMethod() {}

    public static void publicStaticVoidMethod() {}
    private static void privateStaticVoidMethod() {}

    public void publicVoidMethodSurcharged() {}
    public void publicVoidMethodSurcharged(int i) {}
    public void publicVoidMethodSurcharged(java.lang.Object obj) {}
}
