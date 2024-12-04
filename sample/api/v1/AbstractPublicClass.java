package sample.api.v1;

public abstract class AbstractPublicClass {
    public static int publicStaticInt = 0;
    protected static int protectedStaticInt = 0;
    private static int privateStaticInt = 0;

    public int publicInt = 0;
    protected int protectedInt = 0;
    private int privateInt = 0;

    public void publicVoidMethod() {}
    public void publicVoidMethodThrowingCheckedException() throws CheckedException {}
    public void publicVoidMethodThrowingUncheckedException() throws UncheckedException {}

    public abstract void abstractMethod();
    public abstract void abstractMethodThrowingCheckedException() throws CheckedException;
    public abstract void abstractMethodThrowingUncheckedException() throws UncheckedException;
}
