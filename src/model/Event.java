package model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for a calendar event.
 */
public abstract class Event {

    private final String eventId;
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final int durationMinutes;
    private final String detail; // Location for Appointment, Course for Deadline, etc.

    /**
     * Constructs a new Event.
     *
     * @param eventId         Unique ID, or null/blank to generate one.
     * @param title           The event title (non-null).
     * @param description     Optional description.
     * @param startTime       The start date and time (non-null).
     * @param durationMinutes The duration in minutes (must be > 0).
     * @param detail          Optional detail (e.g., location, course).
     */
    public Event(String eventId, String title, String description,
                 LocalDateTime startTime, int durationMinutes, String detail) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank.");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null.");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be positive.");
        }

        this.eventId = (eventId == null || eventId.isBlank()) ? UUID.randomUUID().toString() : eventId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.detail = detail;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        // Null-safe getter
        return Objects.requireNonNullElse(description, "");
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return startTime.plusMinutes(durationMinutes);
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getDetail() {
        // Null-safe getter
        return Objects.requireNonNullElse(detail, "");
    }

    /**
     * Gets the type of event (e.g., "Appointment", "Deadline").
     * Used for serialization.
     *
     * @return The event type as a String.
     */
    public abstract String getType();

    /**
     * Checks if this event's time range conflicts with another event's time range.
     * Events conflict if they overlap (i.e., not if one ends exactly when the other starts).
     *
     * @param other The other event to check against.
     * @return true if the events conflict, false otherwise.
     */
    public boolean conflictsWith(Event other) {
        if (other == null) {
            return false;
        }
        // No conflict if this.start >= other.end OR this.end <= other.start
        // Conflict if (this.start < other.end) AND (this.end > other.start)
        return this.getStartTime().isBefore(other.getEndTime()) &&
                this.getEndTime().isAfter(other.getStartTime());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return eventId.equals(event.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + eventId + '\'' +
                ", title='" + title + '\'' +
                ", start=" + startTime +
                ", duration=" + durationMinutes +
                ", type=" + getType() +
                '}';
    }
}