package sample.api.v1;

import java.util.List;

public class PublicClass {
    public int publicInt = 0;
    private char privateChar = 'c';
    protected boolean protectedBool = false;
    public java.lang.Object publicObject = null;

    public static int publicStaticInt = 0;
    protected static int protectedStaticInt = 0;
    private static int privateStaticInt = 0;

    public final static int publicFinalStaticInt = 0;
    protected final static int protectedFinalStaticInt = 0;
    private final static int privateFinalStaticInt = 0;

    public PublicClass() {}

    protected PublicClass(char valChar) {
        this.privateChar = valChar;
    }

    private PublicClass(int val) {
        this.publicInt = val;
    }

    public PublicClass(int valInt, char valChar, java.lang.Object valObject) {
        this.publicInt = valInt;
        this.privateChar = valChar;
        this.publicObject = valObject;
    }

    public void publicVoidMethod() {}
    protected void protectedVoidMethod() {}
    private void privateVoidMethod() {}
    public java.lang.Object publicObjectMethod() { return null; }

    public static void publicStaticVoidMethod() {}
    protected static void protectedStaticVoidMethod() {}
    private static void privateStaticVoidMethod() {}
    public java.lang.Object publicStaticObjectMethod() { return null; }

    public void publicVoidMethodWithIntParam(int i) {}
    public void publicVoidMethodWithObjectParam(java.lang.Object obj) {}
    public String[] publicStringArrayMethod(String[] array) { return null; }
    public void publicListStringMethod(List<String> test) {}
    public <T extends PublicClassImplementsPublicInterface> void publicGenericMethod(T test, int val) {}
    public void publicVoidMethodWithVarArgs(String... args) {}
    public void publicVoidMethodWithVarArgs(String args) {}
    public void publicVoidMethodWithVarArgsAndInt(int i, String... args) {}
    public void publicVoidMethodWithWildcard(List<?> list) {}
    public void publicVoidMethodWithWildcardExtend(List<? extends List<?>> list) {}
    public PublicFinalClass publicPublicFinalClassMethodWithPublicFinalClassParam(PublicFinalClass cls) { return null; }

    public void publicVoidMethodSurcharged() {}
    public void publicVoidMethodSurcharged(int i) {}
    public void publicVoidMethodSurcharged(java.lang.Object obj) {}

    public native void publicNativeVoidMethod();
}
