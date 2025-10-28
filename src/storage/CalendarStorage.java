package storage;

import model.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarStorage {

    private final List<Event> events;
    private final String dataFilePath = "scheduler_data.txt";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public CalendarStorage() {
        this.events = new ArrayList<>();
        loadData();
    }

    private boolean checkForConflict(Event newEvent) {
        for (Event existingEvent : events) {
            if (existingEvent.getEventId().equals(newEvent.getEventId())) {
                continue; 
            }
            boolean conflict = (newEvent.getStartTime().isBefore(existingEvent.getEndTime()) &&
                                existingEvent.getStartTime().isBefore(newEvent.getEndTime()));
            if (conflict) {
                return true;
            }
        }
        return false;
    }

    public void addEvent(Event event) throws EventConflictException {
        if (checkForConflict(event)) {
            throw new EventConflictException("Event conflicts with an existing event: " + event.getTitle());
        }
        events.add(event);
        Collections.sort(events);
        saveData();
    }

    public void updateEvent(Event updatedEvent) throws EventConflictException {
        if (checkForConflict(updatedEvent)) {
            throw new EventConflictException("Update conflicts with an existing event: " + updatedEvent.getTitle());
        }
        events.removeIf(e -> e.getEventId().equals(updatedEvent.getEventId()));
        events.add(updatedEvent);
        Collections.sort(events);
        saveData();
    }

    public void deleteEvent(String eventId) {
        events.removeIf(e -> e.getEventId().equals(eventId));
        saveData();
    }

    public List<Event> getAllEvents() {
        return new ArrayList<>(events);
    }

    // --- LOGIC FIX 1 ---
    public List<Event> getEventsForDay(LocalDate date) {
        return events.stream()
                .filter(e -> e.getStartTime().toLocalDate().isEqual(date))
                .sorted()
                .collect(Collectors.toList());
    }
    
    // --- LOGIC FIX 2 ---
    public List<Event> getEventsForWeek(LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(7);
        return events.stream()
                .filter(e -> !e.getStartTime().toLocalDate().isBefore(startDate) && e.getStartTime().toLocalDate().isBefore(endDate))
                .sorted()
                .collect(Collectors.toList());
    }

    // --- LOGIC FIX 3 ---
    public List<Event> getEventsForMonth(YearMonth month) {
        return events.stream()
                .filter(e -> e.getStartTime().getYear() == month.getYear() && e.getStartTime().getMonth() == month.getMonth())
                .sorted()
                .collect(Collectors.toList());
    }

    public void saveData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(dataFilePath))) {
            writer.println("# Event Scheduler Data - Format: ID;Type;Title;Description;StartTime;Duration;Detail");
            for (Event event : events) {
                writer.println(event.toCsv());
            }
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    public void loadData() {
        File file = new File(dataFilePath);
        if (!file.exists()) {
            return;
        }
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(";", 7);
                if (parts.length == 7) {
                    try {
                        String id = parts[0];
                        String type = parts[1];
                        String title = parts[2];
                        String description = parts[3];
                        LocalDateTime start = LocalDateTime.parse(parts[4], DATE_TIME_FORMATTER);
                        int duration = Integer.parseInt(parts[5]);
                        String detail = parts[6];

                        Event event = null;
                        if ("APPOINTMENT".equals(type)) {
                            event = new Appointment(id, title, description, start, duration, detail);
                        } else if ("DEADLINE".equals(type)) {
                            event = new Deadline(id, title, description, start, duration, detail);
                        }
                        if (event != null) {
                            events.add(event);
                        }
                    } catch (DateTimeParseException | NumberFormatException e) {
                        System.err.println("Skipping corrupted data line: " + line);
                    }
                }
            }
            Collections.sort(events);
        } catch (FileNotFoundException e) {
            System.err.println("Error loading data: " + e.getMessage());
        }
    }

    public Optional<LocalDateTime> suggestFreeSlot(LocalDateTime desiredStartTime, int durationMinutes) {
        LocalDateTime nextTry = desiredStartTime.plusMinutes(30);

        for (int i = 0; i < 20; i++) { 
            Event testEvent = new Appointment("test", "test", nextTry, durationMinutes, "test");
            if (!checkForConflict(testEvent)) {
                return Optional.of(nextTry);
            }
            nextTry = nextTry.plusMinutes(30);
        }
        return Optional.empty(); 
    }
}

