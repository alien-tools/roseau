package sample.api.v1;

public class PublicClassImplementsPublicInterface implements PublicInterface {
    @Override
    public void voidMethodFromInterface() {}

    @Override
    public boolean booleanMethodFromInterface() { return false; }

    @Override
    public char charMethodFromInterface() { return 'c'; }

    @Override
    public int intMethodFromInterface() { return 0; }

    @Override
    public String stringMethodFromInterface() { return null; }
}
