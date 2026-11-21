package stockly.model;

import java.awt.Color;

/**
 * Stock status classification for a product.
 * <p>
 * The status is always derived automatically from the product quantity
 * (see {@link Product#getStockStatus()}); it can never be set manually,
 * which guarantees the displayed status always matches reality.
 *
 * @author Tooba Akram & Abeeha Noor
 * @version 1.0
 */
public enum StockStatus {

    /** Quantity is zero — nothing left to sell. */
    OUT_OF_STOCK("Out of Stock", new Color(0xC96A63)),

    /** Quantity is between 1 and the low-stock threshold. */
    LOW_STOCK("Low Stock", new Color(0xD9A05B)),

    /** Quantity is above the low-stock threshold. */
    IN_STOCK("In Stock", new Color(0x7C9478));

    private final String label;
    private final Color color;

    StockStatus(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    /** @return short human readable label for badges and legends */
    public String getLabel() {
        return label;
    }

    /** @return the accent color used for badges, charts and indicators */
    public Color getColor() {
        return color;
    }
}
