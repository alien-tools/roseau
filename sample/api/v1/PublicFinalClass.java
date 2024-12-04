package sample.api.v1;

public final class PublicFinalClass {
    public int publicInt = 0;
    private char privateChar = 'c';

    public PublicFinalClass() {}

    public void publicVoidMethod() {}
    private void privateVoidMethod() {}
    public void publicVoidMethodThrowingCheckedException() throws CheckedException {}
    public void publicVoidMethodThrowingUncheckedException() throws UncheckedException {}
    protected void protectedVoidMethodThrowingCheckedException() throws CheckedException {}
    protected void protectedVoidMethodThrowingUncheckedException() throws UncheckedException {}
    private void privateVoidMethodThrowingCheckedException() throws CheckedException {}
    private void privateVoidMethodThrowingUncheckedException() throws UncheckedException {}

    public static void publicStaticVoidMethod() {}
    private static void privateStaticVoidMethod() {}
    public static void publicStaticVoidMethodThrowingCheckedException() throws CheckedException {}
    public static void publicStaticVoidMethodThrowingUncheckedException() throws UncheckedException {}
    protected static void protectedStaticVoidMethodThrowingCheckedException() throws CheckedException {}
    protected static void protectedStaticVoidMethodThrowingUncheckedException() throws UncheckedException {}
    private static void privateStaticVoidMethodThrowingCheckedException() throws CheckedException {}
    private static void privateStaticVoidMethodThrowingUncheckedException() throws UncheckedException {}

    public void publicVoidMethodSurcharged() {}
    public void publicVoidMethodSurcharged(int i) {}
    public void publicVoidMethodSurcharged(java.lang.Object obj) {}
}
