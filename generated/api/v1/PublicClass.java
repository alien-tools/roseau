package generated.api.v1;

public class PublicClass {
    public int publicInt = 0;
    private char privateChar = 'c';
    protected boolean protectedBool = false;

    public PublicClass() {}

    protected PublicClass(char valChar) {
        this.privateChar = valChar;
    }

    private PublicClass(int val) {
        this.publicInt = val;
    }

    public PublicClass(int valInt, char valChar) {
        this.publicInt = valInt;
        this.privateChar = valChar;
    }

    public void publicVoidMethod() {}
    protected void protectedVoidMethod() {}
    private void privateVoidMethod() {}

    public static void publicStaticVoidMethod() {}
    protected static void protectedStaticVoidMethod() {}
    private static void privateStaticVoidMethod() {}
}
