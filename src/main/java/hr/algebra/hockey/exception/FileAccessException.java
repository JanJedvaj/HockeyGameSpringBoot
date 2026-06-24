package hr.algebra.hockey.exception;

public class FileAccessException extends RuntimeException {
    public FileAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
