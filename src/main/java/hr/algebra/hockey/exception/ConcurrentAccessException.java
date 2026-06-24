package hr.algebra.hockey.exception;

public class ConcurrentAccessException extends RuntimeException {
    public ConcurrentAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
