package sample.api.v1;

public class PublicClassWithoutSimpleConstructor {
    private final int intValue;

    public PublicClassWithoutSimpleConstructor(int intValue) {
        this.intValue = intValue;
    }

    public int getIntValue() {
        return intValue;
    }
}
