package stockly.model;

/**
 * Represents a single inventory product.
 * <p>
 * Each product has a unique identifier, a name, and a stock quantity.
 * The stock status is never stored — it is always derived from the
 * quantity via {@link #getStockStatus()} so it can never become
 * inconsistent with the actual quantity.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class Product {

    /**
     * Default low-stock threshold, kept for reference. The live value is
     * {@link AppConfig#getLowStockThreshold()} and can be changed in
     * Settings — it applies to every product immediately.
     */
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private String id;
    private String name;
    private int quantity;
    // Optional user-selected status. When null the status is derived
    // automatically from the quantity (see getStockStatus()).
    private StockStatus manualStatus;

    /**
     * Creates a new product.
     *
     * @param id       unique product identifier, e.g. {@code "P001"}
     * @param name     human readable product name (must not be blank)
     * @param quantity stock quantity (must be {@code >= 0})
     */
    public Product(String id, String name, int quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
    }

    /** @return the unique product identifier */
    public String getId() {
        return id;
    }

    /**
     * Sets the product identifier.
     *
     * @param id the new identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /** @return the product name */
    public String getName() {
        return name;
    }

    /**
     * Sets the product name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the current stock quantity */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns the manually selected stock status, or {@code null} when
     * the status is derived automatically from the quantity.
     *
     * @return the manual status override, or {@code null} for auto mode
     */
    public StockStatus getManualStatus() {
        return manualStatus;
    }

    /**
     * Sets an optional manual status override.
     *
     * @param status the status to pin, or {@code null} to return to
     *               automatic quantity-based classification
     */
    public void setManualStatus(StockStatus status) {
        this.manualStatus = status;
    }

    /**
     * Sets the stock quantity.
     *
     * @param quantity the new quantity (must be {@code >= 0})
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Derives the stock status from the current quantity.
     * <ul>
     *   <li>{@code quantity == 0} &rarr; {@link StockStatus#OUT_OF_STOCK}</li>
     *   <li>{@code 1 <= quantity <= AppConfig threshold} &rarr; {@link StockStatus#LOW_STOCK}</li>
     *   <li>{@code quantity > AppConfig threshold} &rarr; {@link StockStatus#IN_STOCK}</li>
     * </ul>
     *
     * @return the stock status matching the current quantity
     */
    public StockStatus getStockStatus() {
        if (manualStatus != null) {
            return manualStatus;
        }
        if (quantity == 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (quantity <= AppConfig.getLowStockThreshold()) {
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.IN_STOCK;
    }

    /**
     * Returns a human readable representation of this product.
     *
     * @return a string containing the id, name and quantity
     */
    @Override
    public String toString() {
        return "Product{" + "id='" + id + '\'' + ", name='" + name + '\''
                + ", quantity=" + quantity + '}';
    }
}
