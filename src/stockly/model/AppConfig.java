package stockly.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Application-wide preferences persisted to {@code stockly.properties}.
 * <p>
 * Holds user-configurable behaviour such as the low-stock threshold and
 * the delete-confirmation toggle. Values are loaded once at startup and
 * saved whenever the user changes them in Settings, so every screen —
 * which all read these values live — stays consistent.
 *
 * @author Tooba Akram & Abeeha Noor
 * @version 1.0
 */
public final class AppConfig {

    private static final Path FILE = Paths.get("stockly.properties");

    private static int lowStockThreshold = 5;
    private static boolean confirmDelete = true;

    private AppConfig() {
        // utility class
    }

    /** @return quantity at or below which an item counts as low stock */
    public static int getLowStockThreshold() {
        return lowStockThreshold;
    }

    /**
     * Sets the low-stock threshold (clamped to a sane range).
     * Takes effect immediately for every product status calculation.
     *
     * @param threshold new threshold, {@code 0..100}
     */
    public static void setLowStockThreshold(int threshold) {
        lowStockThreshold = Math.max(0, Math.min(100, threshold));
    }

    /** @return whether deletes should ask for confirmation */
    public static boolean isConfirmDelete() {
        return confirmDelete;
    }

    /**
     * Sets whether product deletion requires confirmation.
     *
     * @param confirm {@code true} to always confirm, {@code false} to delete immediately
     */
    public static void setConfirmDelete(boolean confirm) {
        confirmDelete = confirm;
    }

    /** Loads preferences from disk; missing file keeps defaults. */
    public static void load() {
        load(FILE);
    }

    /** Loads preferences from the given file; missing file keeps defaults. */
    public static void load(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
            lowStockThreshold = clamp(Integer.parseInt(
                    props.getProperty("lowStockThreshold", "5")));
            confirmDelete = Boolean.parseBoolean(
                    props.getProperty("confirmDelete", "true"));
        } catch (IOException | NumberFormatException e) {
            System.err.println("Could not load settings: " + e.getMessage());
        }
    }

    /** Saves current preferences to disk. */
    public static void save() {
        save(FILE);
    }

    /** Saves current preferences to the given file. */
    public static void save(Path file) {
        Properties props = new Properties();
        props.setProperty("lowStockThreshold", String.valueOf(lowStockThreshold));
        props.setProperty("confirmDelete", String.valueOf(confirmDelete));
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "STOCKLY settings");
        } catch (IOException e) {
            System.err.println("Could not save settings: " + e.getMessage());
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
