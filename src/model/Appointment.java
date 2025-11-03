package model;

import java.time.LocalDateTime;

/**
 * Represents an appointment, a specific type of Event.
 */
public class Appointment extends Event {

    public Appointment(String eventId, String title, String description,
                       LocalDateTime startTime, int durationMinutes, String detail) {
        super(eventId, title, description, startTime, durationMinutes, detail);
    }

    @Override
    public String getType() {
        return "Appointment";
    }

    /**
     * Convenience getter for detail, representing Location for an Appointment.
     *
     * @return The event location.
     */
    public String getLocation() {
        return getDetail();
    }
}