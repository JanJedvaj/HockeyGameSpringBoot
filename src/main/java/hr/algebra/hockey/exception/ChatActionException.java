package hr.algebra.hockey.exception;

public class ChatActionException extends RuntimeException {
    public ChatActionException(String message, Throwable cause) {
        super(message, cause);
    }
}