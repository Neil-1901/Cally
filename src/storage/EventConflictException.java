package storage;

import model.Event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Exception thrown when attempting to add or update an event causes a time conflict.
 */
public class EventConflictException extends Exception {

    private final List<Event> conflictingEvents;
    private final Optional<LocalDateTime> suggestedSlot;

    public EventConflictException(List<Event> conflictingEvents, Optional<LocalDateTime> suggestedSlot) {
        super();
        this.conflictingEvents = conflictingEvents;
        this.suggestedSlot = suggestedSlot;
    }

    @Override
    public String getMessage() {
        String conflicts = conflictingEvents.stream()
                .map(e -> String.format("'%s' (%s - %s)",
                        e.getTitle(),
                        e.getStartTime().toLocalTime(),
                        e.getEndTime().toLocalTime()))
                .collect(Collectors.joining(", "));

        String message = "Event conflicts with: " + conflicts + ".";

        if (suggestedSlot.isPresent()) {
            String suggestion = suggestedSlot.get().format(DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm"));
            message += "\nSuggested free slot: " + suggestion;
        } else {
            message += "\nNo immediate free slot found near desired time.";
        }

        return message;
    }

    public List<Event> getConflictingEvents() {
        return conflictingEvents;
    }

    public Optional<LocalDateTime> getSuggestedSlot() {
        return suggestedSlot;
    }
}