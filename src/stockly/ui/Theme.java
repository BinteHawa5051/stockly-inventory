package stockly.ui;

import java.awt.Color;
import java.awt.Font;

/**
 * Central design tokens for the Stockly interface.
 * <p>
 * Every color, font and spacing value used across the application is
 * defined here so the visual identity stays consistent and can be
 * retuned in one place.
 *
 * @author Tooba Akram and Abeeha Noor
 * @version 1.0
 */
public final class Theme {

    private Theme() {
        // utility class — no instances
    }

    // ---- Palette ------------------------------------------------------

    /** Warm ivory — main application background. */
    public static final Color BG = new Color(0xF8F6F0);
    /** Card / panel surface. */
    public static final Color SURFACE = new Color(0xFFFFFF);
    /** Slightly recessed surface (table header, inputs). */
    public static final Color SURFACE_ALT = new Color(0xF1EFE7);
    /** Muted sage — primary accent. */
    public static final Color SAGE = new Color(0xA8B5A0);
    /** Deep forest — navigation and primary actions. */
    public static final Color FOREST = new Color(0x34483C);
    /** Dusty rose — secondary accent / danger. */
    public static final Color ROSE = new Color(0xD9A6A0);
    /** Soft peach — highlights / warnings. */
    public static final Color PEACH = new Color(0xF0C6A8);
    /** Muted lavender — secondary data color. */
    public static final Color LAVENDER = new Color(0xC9C0D9);
    /** Charcoal — primary text. */
    public static final Color TEXT = new Color(0x30332F);
    /** Secondary text. */
    public static final Color TEXT_MUTED = new Color(0x7A7D74);
    /** Soft grey — borders and dividers. */
    public static final Color BORDER = new Color(0xE9E8E1);
    /** Hover tint for sidebar items and buttons. */
    public static final Color HOVER = new Color(0xEFEDE4);
    /** Active navigation tint. */
    public static final Color ACTIVE_BG = new Color(0xE4EADF);

    /** Chart colours for the in-stock segment. */
    public static final Color CHART_IN_STOCK = new Color(0x7C9478);
    /** Chart colour for the low-stock segment. */
    public static final Color CHART_LOW_STOCK = new Color(0xD9A05B);
    /** Chart colour for the out-of-stock segment. */
    public static final Color CHART_OUT_STOCK = new Color(0xC96A63);

    /** Coordinated pastel palette used for per-product bars. */
    public static final Color[] CHART_SERIES = {
        new Color(0xA8B5A0), new Color(0xD9A6A0), new Color(0xC9C0D9),
        new Color(0xF0C6A8), new Color(0x8FA98A), new Color(0xB7A8BC),
        new Color(0xE0B9A0), new Color(0x9DB5C9)
    };

    // ---- Typography ---------------------------------------------------

    /** Base UI font family — Segoe UI on Windows, falls back gracefully. */
    public static final String FONT_FAMILY = "Segoe UI";

    /**
     * Returns the regular-weight UI font at the requested size.
     *
     * @param size font size in points
     * @return regular UI font at the given size
     */
    public static Font font(int size) {
        return new Font(FONT_FAMILY, Font.PLAIN, size);
    }

    /**
     * Returns the bold-weight UI font at the requested size.
     *
     * @param size font size in points
     * @return bold UI font at the given size
     */
    public static Font bold(int size) {
        return new Font(FONT_FAMILY, Font.BOLD, size);
    }

    // ---- Metrics ------------------------------------------------------

    /** Sidebar width in pixels. */
    public static final int SIDEBAR_WIDTH = 216;
    /** Standard corner radius used by cards and buttons. */
    public static final int RADIUS = 10;
}
