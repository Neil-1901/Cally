package model;

import java.time.LocalDateTime;

public class Deadline extends Event {

    private String course;

    public Deadline(String title, String description, LocalDateTime startTime, int durationMinutes, String course) {
        super(title, description, startTime, durationMinutes);
        this.course = course;
    }

    public Deadline(String eventId, String title, String description, LocalDateTime startTime, int durationMinutes, String course) {
        super(eventId, title, description, startTime, durationMinutes);
        this.course = course;
    }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    @Override
    public String getType() {
        return "DEADLINE";
    }

    @Override
    public String getDetail() {
        return course;
    }
}
