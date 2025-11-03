package storage;

import model.Appointment;
import model.Deadline;
import model.Event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON serialization/deserialization helper using only core JDK.
 * This is a simple, non-compliant parser strictly for this application's Event model.
 */
public class JsonUtil {

    // Regex to find key-value pairs.
    // Group 1: key
    // Group 3: string value (if present)
    // Group 4: numeric value (if present)
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("\"(.*?)\":\\s*(\"(.*?)\"|(\\d+))");

    /**
     * Serializes a list of Events to a simple JSON array string.
     */
    public static String serialize(List<Event> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            sb.append("  {\n");
            sb.append("    \"eventId\": \"").append(escape(event.getEventId())).append("\",\n");
            sb.append("    \"title\": \"").append(escape(event.getTitle())).append("\",\n");
            sb.append("    \"description\": \"").append(escape(event.getDescription())).append("\",\n");
            sb.append("    \"startTime\": \"").append(escape(event.getStartTime().toString())).append("\",\n");
            sb.append("    \"durationMinutes\": ").append(event.getDurationMinutes()).append(",\n");
            sb.append("    \"detail\": \"").append(escape(event.getDetail())).append("\",\n");
            sb.append("    \"type\": \"").append(escape(event.getType())).append("\"\n");
            sb.append("  }");

            if (i < events.size() - 1) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
        }

        sb.append("]\n");
        return sb.toString();
    }

    /**
     * Deserializes a JSON string into a List of Events.
     */
    public static List<Event> deserialize(String json) {
        List<Event> events = new ArrayList<>();
        if (json == null || json.isBlank() || json.equals("[]")) {
            return events;
        }

        // Simple regex to find content inside { ... }
        Pattern objectPattern = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
        Matcher objectMatcher = objectPattern.matcher(json);

        while (objectMatcher.find()) {
            String objectString = objectMatcher.group(1);
            Map<String, String> map = new HashMap<>();

            Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(objectString);
            while (kvMatcher.find()) {
                String key = kvMatcher.group(1);
                String strVal = kvMatcher.group(3); // String value
                String numVal = kvMatcher.group(4); // Numeric value

                if (strVal != null) {
                    map.put(key, unescape(strVal));
                } else if (numVal != null) {
                    map.put(key, numVal);
                }
            }

            try {
                String type = map.get("type");
                String id = map.getOrDefault("eventId", UUID.randomUUID().toString());
                String title = map.get("title");
                String description = map.get("description");
                LocalDateTime startTime = LocalDateTime.parse(map.get("startTime"));
                int duration = Integer.parseInt(map.get("durationMinutes"));
                String detail = map.get("detail");

                if ("Appointment".equals(type)) {
                    events.add(new Appointment(id, title, description, startTime, duration, detail));
                } else if ("Deadline".equals(type)) {
                    events.add(new Deadline(id, title, description, startTime, duration, detail));
                }
            } catch (Exception e) {
                System.err.println("Failed to parse event object: " + objectString);
                e.printStackTrace();
            }
        }
        return events;
    }

    /**
     * Escapes special JSON characters in a string.
     */
    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Un-escapes special JSON characters.
     */
    private static String unescape(String s) {
        if (s == null) {
            return null;
        }
        return s.replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}