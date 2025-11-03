package model;

import java.time.LocalDateTime;

/**
 * Represents a deadline, a specific type of Event.
 */
public class Deadline extends Event {

    public Deadline(String eventId, String title, String description,
                    LocalDateTime startTime, int durationMinutes, String detail) {
        super(eventId, title, description, startTime, durationMinutes, detail);
    }

    @Override
    public String getType() {
        return "Deadline";
    }

    /**
     * Convenience getter for detail, representing a Course for a Deadline.
     *
     * @return The event course/subject.
     */
    public String getCourse() {
        return getDetail();
    }
}