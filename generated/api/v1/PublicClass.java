package generated.api.v1;

public class PublicClass {
    public int publicInt = 0;
    private char privateChar = 'c';

    public PublicClass() {}

    private PublicClass(int val) {
        this.publicInt = val;
    }

    public PublicClass(int valInt, char valChar) {
        this.publicInt = val;
        this.privateChar = valChar;
    }

    public void publicVoidMethod() {}
}
