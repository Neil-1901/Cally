package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public abstract class Event implements Comparable<Event> {

    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String eventId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private int durationMinutes;

    public Event(String title, String description, LocalDateTime startTime, int durationMinutes) {
        this.eventId = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
    }

    public Event(String eventId, String title, String description, LocalDateTime startTime, int durationMinutes) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
    }

    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public LocalDateTime getEndTime() {
        return startTime.plusMinutes(durationMinutes);
    }

    public abstract String getType();
    public abstract String getDetail();

    public String toCsv() {
        String safeTitle = title.replace(";", ",");
        String safeDescription = description.replace(";", ",");
        String safeDetail = getDetail().replace(";", ",");

        return String.join(";",
                eventId,
                getType(),
                safeTitle,
                safeDescription,
                startTime.format(DATE_TIME_FORMATTER),
                String.valueOf(durationMinutes),
                safeDetail
        );
    }

    @Override
    public int compareTo(Event other) {
        return this.startTime.compareTo(other.startTime);
    }
}
