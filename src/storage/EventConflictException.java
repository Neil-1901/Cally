package storage;

public class EventConflictException extends Exception {
    public EventConflictException(String message) {
        super(message);
    }
}
