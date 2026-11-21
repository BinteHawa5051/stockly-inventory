package stockly.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory log of recent inventory events for the current session.
 * <p>
 * The most recent entries are kept at the front of the list. The log is
 * intentionally session-scoped: entries describe actions the user
 * actually performed while the application was running.
 *
 * @author Tooba Akram &amp; Abeeha Noor
 * @version 1.0
 */
public class ActivityLog {

    /** Maximum number of entries kept in memory. */
    private static final int MAX_ENTRIES = 50;

    private final List<ActivityEntry> entries = new ArrayList<>();

    /**
     * Records a new activity entry at the front of the feed.
     *
     * @param kind        the activity category
     * @param description what happened
     */
    public void record(ActivityEntry.Kind kind, String description) {
        entries.add(0, new ActivityEntry(kind, description));
        if (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
    }

    /**
     * Returns the most recent entries, newest first.
     *
     * @param limit maximum number of entries to return
     * @return unmodifiable list of up to {@code limit} entries
     */
    public List<ActivityEntry> getRecent(int limit) {
        int size = Math.min(limit, entries.size());
        return Collections.unmodifiableList(entries.subList(0, size));
    }

    /** @return total number of recorded entries */
    public int size() {
        return entries.size();
    }
}
