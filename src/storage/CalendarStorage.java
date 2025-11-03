package storage;

import model.Appointment;
import model.Event;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Manages event storage, caching, persistence to JSON, and conflict detection.
 */
public class CalendarStorage {

    private final Map<String, Event> eventCache = new ConcurrentHashMap<>();
    private final Set<String> remindersFired = Collections.synchronizedSet(new HashSet<>());
    private final Path storageFile = Paths.get("events.json");
    private Timer reminderTimer;

    /**
     * Creates a new CalendarStorage instance.
     * Loads events from the storage file, or creates the file if it doesn't exist.
     */
    public CalendarStorage() {
        loadEvents();
    }

    private void loadEvents() {
        synchronized (this) {
            try {
                if (!Files.exists(storageFile)) {
                    Files.writeString(storageFile, "[]");
                }
                String json = Files.readString(storageFile);
                List<Event> events = JsonUtil.deserialize(json);
                eventCache.clear();
                for (Event e : events) {
                    eventCache.put(e.getEventId(), e);
                }
            } catch (IOException e) {
                System.err.println("Failed to load events from file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void persistEvents() {
        synchronized (this) {
            try {
                List<Event> events = new ArrayList<>(eventCache.values());
                // Sort for stable output
                events.sort(Comparator.comparing(Event::getStartTime));
                String json = JsonUtil.serialize(events);
                Files.writeString(storageFile, json);
            } catch (IOException e) {
                System.err.println("Failed to persist events to file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }



    public synchronized List<Event> getAllEvents() {
        return new ArrayList<>(eventCache.values());
    }

    public synchronized List<Event> getEventsForDay(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        // Event overlaps day if: event.start < endOfDay AND event.end > startOfDay
        return eventCache.values().stream()
                .filter(e -> e.getStartTime().isBefore(endOfDay) && e.getEndTime().isAfter(startOfDay))
                .sorted(Comparator.comparing(Event::getStartTime))
                .collect(Collectors.toList());
    }

    public synchronized List<Event> getEventsForWeek(LocalDate startOfWeek) {
        LocalDateTime start = startOfWeek.atStartOfDay();
        LocalDateTime end = startOfWeek.plusDays(7).atStartOfDay();
        // Event overlaps week if: event.start < endOfWeek AND event.end > startOfWeek
        return eventCache.values().stream()
                .filter(e -> e.getStartTime().isBefore(end) && e.getEndTime().isAfter(start))
                .sorted(Comparator.comparing(Event::getStartTime))
                .collect(Collectors.toList());
    }

    public synchronized List<Event> getEventsForMonth(YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        // Event overlaps month if: event.start < endOfMonth AND event.end > startOfMonth
        return eventCache.values().stream()
                .filter(e -> e.getStartTime().isBefore(end) && e.getEndTime().isAfter(start))
                .sorted(Comparator.comparing(Event::getStartTime))
                .collect(Collectors.toList());
    }

    private List<Event> findConflicts(Event e) {
        // Find conflicts, excluding the event itself if it's already in the cache (for updates)
        return eventCache.values().stream()
                .filter(existing -> !existing.getEventId().equals(e.getEventId()) && existing.conflictsWith(e))
                .collect(Collectors.toList());
    }

    public synchronized void addEvent(Event e) throws EventConflictException {
        List<Event> conflicts = findConflicts(e);
        if (!conflicts.isEmpty()) {
            throw new EventConflictException(conflicts, suggestFreeSlot(e.getStartTime(), e.getDurationMinutes()));
        }
        eventCache.put(e.getEventId(), e);
        persistEvents();
    }

    public synchronized void updateEvent(Event e) throws EventConflictException {
        List<Event> conflicts = findConflicts(e);
        if (!conflicts.isEmpty()) {
            throw new EventConflictException(conflicts, suggestFreeSlot(e.getStartTime(), e.getDurationMinutes()));
        }
        eventCache.put(e.getEventId(), e);
        remindersFired.remove(e.getEventId()); // Allow reminder to fire again if rescheduled
        persistEvents();
    }

    public synchronized void deleteEvent(String eventId) {
        eventCache.remove(eventId);
        remindersFired.remove(eventId);
        persistEvents();
    }

    public synchronized Optional<LocalDateTime> suggestFreeSlot(LocalDateTime desiredStart, int durationMinutes) {
        LocalDateTime suggestion = desiredStart.plusMinutes(15); // Start checking 15 mins after conflict

        // Try 100 slots in 15-minute increments
        for (int i = 0; i < 100; i++) {
            LocalDateTime suggestionEnd = suggestion.plusMinutes(durationMinutes);
            
            // Create a dummy event to test
            Event testEvent = new Appointment(null, "test", null, suggestion, durationMinutes, null);

            boolean hasConflict = eventCache.values().stream().anyMatch(e -> e.conflictsWith(testEvent));

            if (!hasConflict) {
                return Optional.of(suggestion);
            }
            suggestion = suggestion.plusMinutes(15);
        }
        return Optional.empty();
    }

    /**
     * Starts the background reminder service.
     * @param reminderCallback The action to perform (on the EDT) when a reminder fires.
     */
    public void startReminderService(Consumer<Event> reminderCallback) {
        if (reminderTimer != null) {
            reminderTimer.cancel();
        }
        reminderTimer = new Timer(true); // Daemon thread
        
        // Check every minute
        reminderTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Must synchronize on the storage object to safely access cache
                synchronized (CalendarStorage.this) {
                    LocalDateTime now = LocalDateTime.now();
                    // We check a 1-minute window:
                    // from 10 minutes from now
                    // to 11 minutes from now
                    LocalDateTime reminderWindowStart = now.plusMinutes(10);
                    LocalDateTime reminderWindowEnd = now.plusMinutes(11);

                    eventCache.values().stream()
                        .filter(e -> !remindersFired.contains(e.getEventId())) // Not already fired
                        .filter(e -> e.getStartTime().isAfter(reminderWindowStart)) // Starts after 10 mins
                        .filter(e -> e.getStartTime().isBefore(reminderWindowEnd)) // Starts before 11 mins
                        .forEach(event -> {
                            reminderCallback.accept(event);
                            remindersFired.add(event.getEventId());
                        });
                }
            }
        }, 0, 60_000); // Run every 60 seconds
    }
}