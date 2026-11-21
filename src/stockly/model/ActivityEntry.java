package stockly.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A single entry in the recent-activity feed.
 * <p>
 * Activity is recorded in memory for the current application session
 * only. It is intentionally not persisted to disk: the feed is meant to
 * show what the user actually did during this run.
 *
 * @author Tooba Akram and Abeeha Noor
 * @version 1.0
 */
public class ActivityEntry {

    /** Visual categories of activity, each mapped to an icon on the UI. */
    public enum Kind {
        /** A new product was added. */
        ADDED,
        /** An existing product was updated. */
        UPDATED,
        /** A product was deleted. */
        DELETED
    }

    private final Kind kind;
    private final String description;
    private final LocalDateTime timestamp;

    /**
     * Creates a new activity entry stamped with the current time.
     *
     * @param kind        the category of activity
     * @param description human readable description of what happened
     */
    public ActivityEntry(Kind kind, String description) {
        this.kind = kind;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Returns the category this entry belongs to.
     *
     * @return the activity category
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the human readable description of what happened.
     *
     * @return human readable description of the event
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the moment this event was recorded.
     *
     * @return when the event occurred
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Formats the timestamp for display, e.g. {@code "14:32"} today or
     * {@code "Yesterday 09:15"} for older entries.
     *
     * @return formatted time string
     */
    public String getTimeLabel() {
        LocalDateTime now = LocalDateTime.now();
        if (timestamp.toLocalDate().equals(now.toLocalDate())) {
            return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        if (timestamp.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            return "Yesterday " + timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        return timestamp.format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"));
    }
}
