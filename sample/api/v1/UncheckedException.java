package sample.api.v1;

public class UncheckedException extends RuntimeException {
    public UncheckedException(String message) {
        super(message);
    }
}
