package sample.api.v1;

public interface PublicInterface {
    int interfaceField = 0;

    static int publicStaticInt = 0;
    static final int publicStaticFinalInt = 0;

    static void staticMethodFromInterface() {}

    boolean booleanMethodFromInterface();
    char charMethodFromInterface();
    int intMethodFromInterface();
    String stringMethodFromInterface();
    void voidMethodFromInterface();
}
