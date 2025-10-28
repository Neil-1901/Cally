package model;

import java.time.LocalDateTime;

public class Appointment extends Event {

    private String location;

    public Appointment(String title, String description, LocalDateTime startTime, int durationMinutes, String location) {
        super(title, description, startTime, durationMinutes);
        this.location = location;
    }

    public Appointment(String eventId, String title, String description, LocalDateTime startTime, int durationMinutes, String location) {
        super(eventId, title, description, startTime, durationMinutes);
        this.location = location;
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    @Override
    public String getType() {
        return "APPOINTMENT";
    }

    @Override
    public String getDetail() {
        return location;
    }
}
